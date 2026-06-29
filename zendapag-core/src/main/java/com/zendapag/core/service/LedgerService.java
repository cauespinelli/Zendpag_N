package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.TransactionStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Razão financeiro (motor): movimenta saldo das contas e registra lançamentos
 * de transação. Centraliza o crédito/débito para que pagamento, saque e
 * settlement compartilhem a mesma lógica de saldo + auditoria.
 *
 * Sandbox: tudo síncrono e em uma transação. Sem PSP — a confirmação do
 * pagamento é simulada pelo endpoint de aprovação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PayoutPolicyService payoutPolicyService;
    private final AutoPayoutService autoPayoutService;

    /**
     * Modo rápido de DEV: interpreta o D+N como N SEGUNDOS (em vez de dias), para
     * validar a liberação pendente -> disponível sem esperar dias reais.
     * Ligar com -Dzendapag.payout.fast-mode=true (ou no application-dev).
     */
    @Value("${zendapag.payout.fast-mode:false}")
    private boolean fastMode;

    /**
     * Liquida um pagamento aprovado segundo a regra efetiva do método
     * (PayoutPolicyService): sem retenção credita o líquido em DISPONÍVEL (D+0);
     * com retenção credita em PENDENTE e agenda a liberação (availableAt = D+N).
     * Registra o crédito (PAYMENT) e a taxa (FEE, receita da plataforma).
     */
    @Transactional
    public void settleApprovedPayment(Payment payment, BigDecimal gross, BigDecimal fee, BigDecimal net) {
        Merchant merchant = payment.getMerchant();

        Account account = accountRepository.findByMerchant(merchant).stream()
            .findFirst()
            .orElseThrow(() -> new BusinessException(
                "Estabelecimento sem conta para crédito: " + merchant.getId()));

        PaymentMethodType method = methodTypeOf(payment);
        PayoutPolicyService.EffectiveRule rule = payoutPolicyService.resolve(merchant, method);

        // 1) Lançamento do crédito do pagamento (líquido ao estabelecimento)
        Transaction credit = new Transaction("TXN-" + payment.getReferenceId(), merchant, payment,
            TransactionType.PAYMENT, gross);
        credit.setAccount(account);
        credit.setFeeAmount(fee);
        credit.setNetAmount(net);
        credit.setMethodType(method);
        credit.setSource(merchant.getSource());
        credit.setStatus(TransactionStatus.COMPLETED);

        if (rule.retentionEnabled()) {
            // Retenção: vai para PENDENTE e agenda a liberação no vencimento
            Instant availableAt = computeAvailableAt(payment, rule);
            BigDecimal before = account.getPendingBalance() != null ? account.getPendingBalance() : BigDecimal.ZERO;
            account.setPendingBalance(before.add(net));
            credit.setReleased(false);
            credit.setAvailableAt(availableAt);
            credit.setDescription("Pagamento aprovado — crédito PENDENTE (" + method
                + " D+" + rule.holdingDays() + ", libera em " + availableAt + ")");
            log.info("Saldo PENDENTE do estabelecimento {} +{} (libera em {}) — {} D+{}",
                merchant.getId(), net, availableAt, method, rule.holdingDays());
        } else {
            // Sem retenção (D+0): cai direto em DISPONÍVEL
            BigDecimal before = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            account.setBalance(before.add(net));
            credit.setReleased(true);
            credit.setAvailableAt(Instant.now());
            credit.setDescription("Pagamento aprovado — crédito DISPONÍVEL (" + method + " D+0)");
            log.info("Saldo DISPONÍVEL do estabelecimento {} +{} (D+0) — {}",
                merchant.getId(), net, method);
        }
        accountRepository.save(account);
        transactionRepository.save(credit);

        // 2) Receita da plataforma (taxa / MDR)
        Transaction platformFee = new Transaction("FEE-" + payment.getReferenceId(), merchant, payment,
            TransactionType.FEE, fee);
        platformFee.setAccount(account);
        platformFee.setStatus(TransactionStatus.COMPLETED);
        platformFee.setReleased(true);
        platformFee.setMethodType(method);
        platformFee.setSource(merchant.getSource());
        platformFee.setDescription("Taxa da plataforma (MDR) sobre o pagamento");
        transactionRepository.save(platformFee);

        log.info("Lançamentos registrados: PAYMENT {} (líquido {}, retencao={}) + FEE {} (receita)",
            credit.getReferenceId(), net, rule.retentionEnabled(), fee);

        // 3) Auto-payout: só faz sentido quando o saldo já está DISPONÍVEL (D+0).
        // Com retenção, o disparo acontece na liberação (BalanceReleaseService).
        if (!rule.retentionEnabled()) {
            autoPayoutService.maybeAutoPayout(merchant, account, net, method, rule.autoPayoutEnabled());
        }
    }

    /** Tipo do método do pagamento; sem método associado, assume PIX (caso padrão). */
    private PaymentMethodType methodTypeOf(Payment payment) {
        if (payment.getPaymentMethod() != null && payment.getPaymentMethod().getType() != null) {
            return payment.getPaymentMethod().getType();
        }
        return PaymentMethodType.PIX;
    }

    /** Vencimento da retenção: paidAt + D+N (ou +N segundos no fast-mode de DEV). */
    private Instant computeAvailableAt(Payment payment, PayoutPolicyService.EffectiveRule rule) {
        Instant base = payment.getPaidAt() != null ? payment.getPaidAt() : Instant.now();
        return fastMode
            ? base.plusSeconds(rule.holdingDays())
            : base.plus(rule.holdingDays(), ChronoUnit.DAYS);
    }

    /**
     * Reverte o crédito de um pagamento estornado, ciente do balde onde o líquido
     * está: se o pagamento ainda estava PENDENTE (lançamento não liberado), debita
     * o pendingBalance; se já tinha sido liberado, debita o disponível. A taxa (MDR)
     * já cobrada permanece como receita da plataforma (política comum de estorno).
     *
     * Borda (sandbox): se o líquido já tiver saído por auto-payout, o disponível
     * pode ficar negativo — registramos o débito mesmo assim e logamos o alerta;
     * tratamento de débito/chargeback real fica para a fase do PSP.
     */
    @Transactional
    public void reverseRefund(Payment payment, BigDecimal net) {
        Merchant merchant = payment.getMerchant();
        Account account = accountRepository.findByMerchant(merchant).stream()
            .findFirst()
            .orElseThrow(() -> new BusinessException(
                "Estabelecimento sem conta para débito: " + merchant.getId()));

        BigDecimal amount = net != null ? net : BigDecimal.ZERO;

        // Descobre o balde do líquido a partir do lançamento original do pagamento.
        Transaction origin = transactionRepository.findByReferenceId("TXN-" + payment.getReferenceId()).orElse(null);
        boolean stillPending = origin != null && Boolean.FALSE.equals(origin.getReleased());

        if (stillPending) {
            BigDecimal before = account.getPendingBalance() != null ? account.getPendingBalance() : BigDecimal.ZERO;
            account.setPendingBalance(before.subtract(amount));
            if (origin != null) {
                origin.setReleased(true); // consome o pendente: não será mais liberado
                transactionRepository.save(origin);
            }
            log.info("Estorno (PENDENTE) — saldo pendente do estabelecimento {}: {} -> {} (-{})",
                merchant.getId(), before, account.getPendingBalance(), amount);
        } else {
            BigDecimal before = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            account.setBalance(before.subtract(amount));
            if (account.getBalance().signum() < 0) {
                log.warn("Estorno deixou o saldo DISPONÍVEL negativo no estabelecimento {} ({}). "
                    + "Líquido provavelmente já sacado (auto-payout).", merchant.getId(), account.getBalance());
            }
            log.info("Estorno (DISPONÍVEL) — saldo do estabelecimento {}: {} -> {} (-{})",
                merchant.getId(), before, account.getBalance(), amount);
        }
        accountRepository.save(account);

        Transaction refund = new Transaction("REF-" + payment.getReferenceId(), merchant, payment,
            TransactionType.REFUND, amount);
        refund.setAccount(account);
        refund.setNetAmount(amount.negate());
        refund.setReleased(true);
        refund.setMethodType(methodTypeOf(payment));
        refund.setSource(merchant.getSource());
        refund.setStatus(TransactionStatus.COMPLETED);
        refund.setDescription("Estorno do pagamento — débito do líquido (" + (stillPending ? "pendente" : "disponível") + ")");
        transactionRepository.save(refund);
    }
}
