package com.zendapag.core.service;

import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Conciliação: confere se cada pagamento APROVADO tem um lançamento PAYMENT
 * correspondente no razão, com o mesmo valor. Aponta divergências (pagamento
 * aprovado sem lançamento, ou valor divergente).
 *
 * Sandbox: a "fonte externa" seria o extrato do PSP; sem PSP, conferimos a
 * consistência interna (pagamentos x razão). O ponto de troca para o extrato
 * real do provedor fica isolado aqui.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public ReconciliationResult reconcileApprovedPayments() {
        List<Payment> approved = paymentRepository.findAllByStatus(PaymentStatus.APPROVED);

        int matched = 0;
        List<Discrepancy> discrepancies = new ArrayList<>();
        BigDecimal internalTotal = BigDecimal.ZERO;
        BigDecimal ledgerTotal = BigDecimal.ZERO;

        for (Payment p : approved) {
            BigDecimal amount = orZero(p.getAmount());
            internalTotal = internalTotal.add(amount);

            Transaction paymentTxn = transactionRepository.findByPayment(p).stream()
                .filter(t -> t.getType() == TransactionType.PAYMENT)
                .findFirst()
                .orElse(null);

            if (paymentTxn == null) {
                discrepancies.add(new Discrepancy(p.getReferenceId(), "MISSING_LEDGER",
                    "Pagamento APROVADO sem lançamento no razão"));
                continue;
            }

            BigDecimal txnAmount = orZero(paymentTxn.getAmount());
            ledgerTotal = ledgerTotal.add(txnAmount);
            if (txnAmount.compareTo(amount) != 0) {
                discrepancies.add(new Discrepancy(p.getReferenceId(), "AMOUNT_MISMATCH",
                    "Valor diverge: pagamento " + amount + " x razão " + txnAmount));
            } else {
                matched++;
            }
        }

        ReconciliationResult result = new ReconciliationResult(
            approved.size(), matched, discrepancies, internalTotal, ledgerTotal);
        log.info("Conciliação: {} aprovados, {} conciliados, {} divergências (interno {}, razão {})",
            approved.size(), matched, discrepancies.size(), internalTotal, ledgerTotal);
        return result;
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public record Discrepancy(String referenceId, String type, String detail) {}

    public record ReconciliationResult(
        int approvedCount,
        int matchedCount,
        List<Discrepancy> discrepancies,
        BigDecimal internalTotal,
        BigDecimal ledgerTotal) {}
}
