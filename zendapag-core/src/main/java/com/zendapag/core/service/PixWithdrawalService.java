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

    @Transactional
    public PixWithdrawalResponse createWithdrawal(Long accountId, UUID merchantId, CreatePixWithdrawalRequest request) {
        log.info("Creating PIX withdrawal");
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        BigDecimal balance = account.getBalance();
        if (balance == null || balance.compareTo(request.getAmount()) < 0) throw new BusinessException("Insufficient balance");
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
        w.cancel(reason);
        PixWithdrawal saved = withdrawalRepository.save(w);
        webhookService.notifyMerchant(saved.getMerchant(), "WITHDRAWAL_FAILED", withdrawalPayload(saved, "WITHDRAWAL_FAILED"));
        return convertToResponse(saved);
    }

    /**
     * Aprova um saque pendente (ação do Admin Master), movendo-o para processamento.
     * Dispara o webhook WITHDRAWAL_COMPLETED.
     */
    @Transactional
    public PixWithdrawalResponse approveWithdrawal(UUID id) {
        PixWithdrawal w = withdrawalRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found: " + id));
        if (w.getStatus() != WithdrawalStatus.PENDING) {
            throw new BusinessException("Only PENDING withdrawals can be approved");
        }
        w.startProcessing();
        PixWithdrawal saved = withdrawalRepository.save(w);
        webhookService.notifyMerchant(saved.getMerchant(), "WITHDRAWAL_COMPLETED", withdrawalPayload(saved, "WITHDRAWAL_COMPLETED"));
        return convertToResponse(saved);
    }

    private java.util.Map<String, Object> withdrawalPayload(PixWithdrawal w, String eventType) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("event", eventType);
        body.put("withdrawal_id", w.getId().toString());
        body.put("reference_id", w.getReferenceId());
        body.put("status", w.getStatus().name());
        body.put("amount", w.getAmount());
        body.put("net", w.getNetAmount());
        body.put("merchant_id", w.getMerchant().getId().toString());
        return body;
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
        r.setStatus(w.getStatus()); r.setDescription(w.getDescription());
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
