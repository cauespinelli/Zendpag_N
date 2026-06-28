package com.zendapag.core.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Libera saldo retido: move o líquido de PENDENTE -> DISPONÍVEL quando o
 * vencimento (availableAt) chega. Cada liberação registra um lançamento RELEASE
 * de auditoria e marca o PAYMENT como released.
 *
 * Acionado pelo scheduler (releaseDuePending) e, em DEV, pela liberação forçada
 * (forceReleaseAllPending) que simula a passagem do tempo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceReleaseService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PayoutPolicyService payoutPolicyService;
    private final AutoPayoutService autoPayoutService;

    /** Libera os lançamentos cujo availableAt já passou. Retorna quantos foram liberados. */
    @Transactional
    public int releaseDuePending() {
        return release(transactionRepository.findDueForRelease(Instant.now()), "vencido");
    }

    /** DEV: libera TODOS os pendentes, ignorando o vencimento (simula passagem do tempo). */
    @Transactional
    public int forceReleaseAllPending() {
        return release(transactionRepository.findAllPendingUnreleased(), "forçado");
    }

    private int release(List<Transaction> due, String motivo) {
        int released = 0;
        for (Transaction t : due) {
            Account account = t.getAccount();
            BigDecimal net = t.getNetAmount() != null ? t.getNetAmount() : t.getAmount();
            if (account == null || net == null) {
                log.warn("[BalanceRelease] lançamento {} sem conta/valor — pulado", t.getReferenceId());
                continue;
            }

            // Move pendente -> disponível
            BigDecimal pending = account.getPendingBalance() != null ? account.getPendingBalance() : BigDecimal.ZERO;
            BigDecimal available = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            account.setPendingBalance(pending.subtract(net));
            account.setBalance(available.add(net));
            accountRepository.save(account);

            // Marca o PAYMENT como liberado
            t.setReleased(true);
            transactionRepository.save(t);

            // Lançamento RELEASE (auditoria)
            String paymentRef = t.getPayment() != null ? t.getPayment().getReferenceId() : t.getReferenceId();
            Transaction rel = new Transaction("REL-" + paymentRef, t.getMerchant(), t.getPayment(),
                TransactionType.RELEASE, net);
            rel.setAccount(account);
            rel.setNetAmount(net);
            rel.setReleased(true);
            rel.setStatus(TransactionStatus.COMPLETED);
            rel.setDescription("Liberação de saldo (" + motivo + ") — pendente -> disponível");
            transactionRepository.save(rel);

            log.info("[BalanceRelease] {} liberado ({}): conta {} pendente {} -> {}, disponível {} -> {}",
                paymentRef, motivo, account.getId(), pending, account.getPendingBalance(),
                available, account.getBalance());
            released++;

            // Auto-payout do saldo recém-liberado, se a regra do método permitir.
            Merchant merchant = t.getMerchant();
            PaymentMethodType method = methodTypeOf(t.getPayment());
            PayoutPolicyService.EffectiveRule rule = payoutPolicyService.resolve(merchant, method);
            if (rule.autoPayoutEnabled()) {
                autoPayoutService.maybeAutoPayout(merchant, account, net, method, true);
            }
        }
        return released;
    }

    /** Tipo do método do pagamento; sem método associado, assume PIX (caso padrão). */
    private PaymentMethodType methodTypeOf(Payment payment) {
        if (payment != null && payment.getPaymentMethod() != null && payment.getPaymentMethod().getType() != null) {
            return payment.getPaymentMethod().getType();
        }
        return PaymentMethodType.PIX;
    }
}
