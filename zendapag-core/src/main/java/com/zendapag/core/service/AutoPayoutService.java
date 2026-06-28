package com.zendapag.core.service;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.TransactionStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.entity.enums.WithdrawalTriggerType;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.PixWithdrawalRepository;
import com.zendapag.core.repository.TransactionRepository;
import com.zendapag.core.service.payout.PayoutProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saque automático (auto-payout): quando há saldo DISPONÍVEL e a regra do método
 * permite, cria um PixWithdrawal AUTOMATIC para a chave PIX da conta do
 * estabelecimento, envia pelo PayoutProvider (sandbox por ora), debita o saldo
 * e registra o lançamento WITHDRAWAL.
 *
 * Acionado em dois pontos: no D+0 (LedgerService) e na liberação de saldo retido
 * (BalanceReleaseService).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoPayoutService {

    private final PixWithdrawalRepository withdrawalRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PayoutProvider payoutProvider;

    /**
     * Dispara o auto-payout se habilitado. Não lança: qualquer bloqueio (sem
     * chave, master switch off, saldo insuficiente) apenas registra log e retorna.
     */
    @Transactional
    public void maybeAutoPayout(Merchant merchant, Account account, BigDecimal amount,
                                PaymentMethodType method, boolean autoPayoutEnabled) {
        if (!autoPayoutEnabled) {
            return;
        }
        if (merchant == null) {
            return;
        }
        // Master switch do estabelecimento
        if (Boolean.FALSE.equals(merchant.getAutoSettle())) {
            log.info("[AutoPayout] desligado pelo master switch (autoSettle=false) — merchant {}", merchant.getId());
            return;
        }
        if (account == null || account.getPixKey() == null || account.getPixKey().isBlank()) {
            log.warn("[AutoPayout] pulado: conta sem chave PIX cadastrada — merchant {}", merchant.getId());
            return;
        }
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(amount) < 0) {
            log.warn("[AutoPayout] saldo disponível {} < valor {} — pulado (merchant {})",
                balance, amount, merchant.getId());
            return;
        }

        String pixKeyType = account.getPixKeyType() != null ? account.getPixKeyType().name() : "RANDOM";
        PixWithdrawal w = new PixWithdrawal(generateReferenceId(), account, merchant, amount,
            account.getPixKey(), pixKeyType);
        w.setTriggerType(WithdrawalTriggerType.AUTOMATIC);
        w.setFeeAmount(BigDecimal.ZERO);
        w.setNetAmount(amount);
        w.setRecipientName(merchant.getName());
        w.setDescription("Saque automático (auto-payout) — " + method);
        w.setBalanceBefore(balance);
        w.startProcessing();

        PayoutProvider.PayoutResult result = payoutProvider.send(w);
        if (!result.success()) {
            w.markAsFailed(result.message());
            withdrawalRepository.save(w);
            log.warn("[AutoPayout] FALHOU ref={} merchant={}: {}", w.getReferenceId(), merchant.getId(), result.message());
            return;
        }

        // Sucesso: debita o disponível, conclui o saque e registra no razão
        account.setBalance(balance.subtract(amount));
        accountRepository.save(account);

        w.complete(result.endToEndId());
        w.setBalanceAfter(account.getBalance());
        withdrawalRepository.save(w);

        Transaction tx = new Transaction("WDR-" + w.getReferenceId(), merchant, TransactionType.WITHDRAWAL, amount);
        tx.setAccount(account);
        tx.setNetAmount(amount.negate());
        tx.setReleased(true);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setDescription("Saque automático para chave PIX do estabelecimento (e2e " + result.endToEndId() + ")");
        transactionRepository.save(tx);

        log.info("[AutoPayout] OK ref={} valor={} merchant={} — saldo disponível {} -> {}",
            w.getReferenceId(), amount, merchant.getId(), balance, account.getBalance());
    }

    private String generateReferenceId() {
        return "AUTO" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
