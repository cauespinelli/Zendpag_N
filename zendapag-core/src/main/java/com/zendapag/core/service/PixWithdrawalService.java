package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.dto.request.CreatePixWithdrawalRequest;
import com.zendapag.core.dto.response.PixWithdrawalResponse;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.entity.enums.WithdrawalStatus;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PixWithdrawalRepository;
import com.zendapag.core.service.payout.PayoutProvider;
import com.zendapag.core.util.WithdrawalPayloads;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PixWithdrawalService {

    private final PixWithdrawalRepository withdrawalRepository;
    private final AccountRepository accountRepository;
    private final MerchantRepository merchantRepository;
    private final WebhookService webhookService;
    private final LedgerService ledgerService;
    private final PayoutProvider payoutProvider;

    @Transactional
    public PixWithdrawalResponse createWithdrawal(Long accountId, UUID merchantId, CreatePixWithdrawalRequest request) {
        log.info("Creating PIX withdrawal");
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        // Só o saldo DISPONÍVEL pode ser sacado — o pendente (em retenção) não conta.
        BigDecimal balance = account.getBalance();
        if (balance == null || balance.compareTo(request.getAmount()) < 0) {
            BigDecimal pending = account.getPendingBalance() != null ? account.getPendingBalance() : BigDecimal.ZERO;
            BigDecimal available = balance != null ? balance : BigDecimal.ZERO;
            throw new BusinessException("Saldo disponível insuficiente: disponível R$ " + available
                + (pending.signum() > 0 ? " (R$ " + pending + " ainda pendente de liberação)" : ""));
        }
        validateDailyLimits(account, request.getAmount());
        PixWithdrawal w = new PixWithdrawal(generateReferenceId(), account, merchant, request.getAmount(), request.getPixKey(), request.getPixKeyType());
        w.setDescription(request.getDescription());
        w.setExternalReference(request.getExternalReference());
        w.setRecipientName(request.getRecipientName());
        w.setRecipientDocument(request.getRecipientDocument());
        w.setBalanceBefore(balance);
        w.setBalanceAfter(balance.subtract(request.getAmount()));
        w.setFeeAmount(calculateFee(merchant, request.getAmount()));
        return convertToResponse(withdrawalRepository.save(w));
    }

    @Transactional(readOnly = true)
    public Optional<PixWithdrawal> findById(UUID id) { return withdrawalRepository.findById(id); }

    @Transactional(readOnly = true)
    public Optional<PixWithdrawal> findByReferenceId(String referenceId) { return withdrawalRepository.findByReferenceId(referenceId); }

    @Transactional(readOnly = true)
    public Page<PixWithdrawalResponse> findByAccount(Long accountId, PageRequest pr) {
        Account a = accountRepository.findById(accountId).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return withdrawalRepository.findByAccount(a, pr).map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PixWithdrawalResponse> findByMerchant(UUID merchantId, PageRequest pr) {
        Merchant m = merchantRepository.findById(merchantId).orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        return withdrawalRepository.findByMerchant(m, pr).map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PixWithdrawalResponse> findByStatus(WithdrawalStatus status, PageRequest pr) {
        return withdrawalRepository.findAll((r, q, cb) -> cb.equal(r.get("status"), status), pr).map(this::convertToResponse);
    }

    @Transactional
    public PixWithdrawalResponse cancelWithdrawal(UUID id, String reason) {
        PixWithdrawal w = withdrawalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        if (!w.canBeCancelled()) throw new BusinessException("Cannot cancel");
        // Se já tinha sido debitado (estava em PROCESSING), estorna o saldo de volta.
        boolean wasDebited = w.getStatus() == WithdrawalStatus.PROCESSING;
        if (wasDebited) {
            ledgerService.reverseWithdrawal(w.getAccount(), w.getAmount(), w.getReferenceId(), w.getMerchant().getSource());
        }
        w.cancel(reason);
        PixWithdrawal saved = withdrawalRepository.save(w);
        fireWithdrawalEvent(saved, "WITHDRAWAL_FAILED");
        return convertToResponse(saved);
    }

    /**
     * Aprova um saque PENDING (Admin Master). Ciclo correto, com status sempre
     * batendo com o evento:
     *   PENDING -> PROCESSING: DEBITA o disponível (reserva + ledger) e dispara
     *              WITHDRAWAL_PROCESSING. A revalidação do saldo aqui impede dupla-saque.
     *   -> envia ao provedor (sandbox: sucesso imediato):
     *        sucesso -> COMPLETED  + WITHDRAWAL_COMPLETED
     *        falha   -> FAILED     + ESTORNA o saldo + WITHDRAWAL_FAILED
     */
    @Transactional
    public PixWithdrawalResponse approveWithdrawal(UUID id) {
        PixWithdrawal w = withdrawalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found: " + id));
        if (w.getStatus() != WithdrawalStatus.PENDING) {
            throw new BusinessException("Apenas saques PENDING podem ser aprovados (atual: " + w.getStatus() + ")");
        }
        Account account = w.getAccount();
        Merchant merchant = w.getMerchant();
        BigDecimal amount = w.getAmount();
        String source = merchant.getSource();

        // PENDING -> PROCESSING: debita o disponível (revalida -> trava dupla-saque)
        BigDecimal before = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        w.startProcessing();
        w.setBalanceBefore(before);
        ledgerService.debitForWithdrawal(account, amount, w.getReferenceId(), source);
        w.setBalanceAfter(account.getBalance());
        withdrawalRepository.save(w);
        fireWithdrawalEvent(w, "WITHDRAWAL_PROCESSING");

        // Envia ao provedor (sandbox: síncrono)
        PayoutProvider.PayoutResult result = payoutProvider.send(w);
        if (result.success()) {
            w.complete(result.endToEndId());           // PROCESSING -> COMPLETED
            withdrawalRepository.save(w);
            fireWithdrawalEvent(w, "WITHDRAWAL_COMPLETED");
        } else {
            ledgerService.reverseWithdrawal(account, amount, w.getReferenceId(), source); // estorna
            w.markAsFailed(result.message());          // -> FAILED
            w.setBalanceAfter(account.getBalance());
            withdrawalRepository.save(w);
            fireWithdrawalEvent(w, "WITHDRAWAL_FAILED");
        }
        return convertToResponse(w);
    }

    /**
     * Conclui um saque a partir da confirmação do PSP (webhook de entrada
     * withdrawal.completed). PROCESSING -> COMPLETED + WITHDRAWAL_COMPLETED.
     * Idempotente: se já COMPLETED, não repete.
     */
    @Transactional
    public void completeWithdrawalFromPsp(String referenceId, String endToEndId) {
        PixWithdrawal w = withdrawalRepository.findByReferenceId(referenceId)
            .orElseThrow(() -> new ResourceNotFoundException("Saque não encontrado: " + referenceId));
        if (w.getStatus() == WithdrawalStatus.COMPLETED) {
            return; // idempotente
        }
        // Se ainda não passou por PROCESSING (não debitado), debita agora.
        if (w.getStatus() == WithdrawalStatus.PENDING) {
            BigDecimal before = w.getAccount().getBalance() != null ? w.getAccount().getBalance() : BigDecimal.ZERO;
            w.startProcessing();
            w.setBalanceBefore(before);
            ledgerService.debitForWithdrawal(w.getAccount(), w.getAmount(), w.getReferenceId(), w.getMerchant().getSource());
            w.setBalanceAfter(w.getAccount().getBalance());
        }
        w.complete(endToEndId != null ? endToEndId : "PSP-confirm");
        withdrawalRepository.save(w);
        fireWithdrawalEvent(w, "WITHDRAWAL_COMPLETED");
    }

    /** Dispara o webhook do saque (ao merchant e à origem), com o status atual = nome do evento. */
    private void fireWithdrawalEvent(PixWithdrawal w, String eventType) {
        webhookService.notifyMerchant(w.getMerchant(), eventType, WithdrawalPayloads.of(w, eventType));
        webhookService.notifyOrigin(w.getMerchant(), eventType, WithdrawalPayloads.of(w, eventType));
    }

    @Transactional
    public void processPixWithdrawal(String wid) throws BusinessException {
        PixWithdrawal w = withdrawalRepository.findByReferenceId(wid).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        if (w.getStatus() != WithdrawalStatus.PENDING) throw new BusinessException("Only PENDING can be processed");
        w.startProcessing();
        withdrawalRepository.save(w);
    }

    public void validateWithdrawalRequest(String wid) throws ResourceNotFoundException {
        PixWithdrawal w = withdrawalRepository.findByReferenceId(wid).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        if (w.isExpired()) throw new BusinessException("Expired");
        if (w.isFinal()) throw new BusinessException("Final status");
    }

    public PixWithdrawalResponse convertToResponse(PixWithdrawal w) {
        PixWithdrawalResponse r = new PixWithdrawalResponse();
        r.setId(w.getId()); r.setReferenceId(w.getReferenceId()); r.setAmount(w.getAmount());
        r.setFeeAmount(w.getFeeAmount()); r.setNetAmount(w.getNetAmount()); r.setPixKey(w.getPixKey());
        r.setPixKeyType(w.getPixKeyType()); r.setRecipientName(w.getRecipientName());
        r.setRecipientDocument(w.getRecipientDocument()); r.setRecipientBank(w.getRecipientBank());
        r.setStatus(w.getStatus()); r.setTriggerType(w.getTriggerType()); r.setDescription(w.getDescription());
        r.setRejectionReason(w.getRejectionReason()); r.setPixTransactionId(w.getPixTransactionId());
        r.setPixEndToEndId(w.getPixEndToEndId()); r.setRequestedAt(w.getRequestedAt());
        r.setProcessedAt(w.getProcessedAt()); r.setCompletedAt(w.getCompletedAt());
        r.setCancelledAt(w.getCancelledAt()); r.setExpiresAt(w.getExpiresAt());
        r.setBalanceBefore(w.getBalanceBefore()); r.setBalanceAfter(w.getBalanceAfter());
        r.setMetadata(w.getMetadata()); r.setCreatedAt(w.getCreatedAt()); r.setUpdatedAt(w.getUpdatedAt());
        return r;
    }

    private String generateReferenceId() { return "WD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }

    private void validateDailyLimits(Account a, BigDecimal amt) {
        Instant today = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        if (withdrawalRepository.countTodayWithdrawalsByAccount(a, today) >= 10) throw new BusinessException("Daily limit");
        BigDecimal sum = withdrawalRepository.sumTodayWithdrawalAmountByAccount(a, today);
        if (sum == null) sum = BigDecimal.ZERO;
        if (sum.add(amt).compareTo(new BigDecimal("50000.00")) > 0) throw new BusinessException("Amount limit");
    }

    private BigDecimal calculateFee(Merchant m, BigDecimal amt) { return new BigDecimal("2.50"); }
}
