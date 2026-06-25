package com.zendapag.core.service;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes da conciliação: cada pagamento APROVADO deve ter um lançamento PAYMENT
 * de mesmo valor no razão. Detecta MISSING_LEDGER e AMOUNT_MISMATCH.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationService — conciliação interna")
class ReconciliationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private ReconciliationService reconciliationService;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = new Merchant("Loja Teste", "12345678000190", "loja@teste.com");
        merchant.setId(UUID.randomUUID());
    }

    private Payment approved(String ref, String amount) {
        Payment p = new Payment(ref, merchant, new BigDecimal(amount));
        p.setId(UUID.randomUUID());
        p.setStatus(PaymentStatus.APPROVED);
        return p;
    }

    private Transaction paymentTxn(Payment p, String amount) {
        return new Transaction("TXN-" + p.getReferenceId(), merchant, p, TransactionType.PAYMENT, new BigDecimal(amount));
    }

    @Test
    @DisplayName("pagamento aprovado com lançamento de mesmo valor → concilia, 0 divergências")
    void tudoConcilia() {
        Payment p = approved("PAY-1", "100.00");
        when(paymentRepository.findAllByStatus(PaymentStatus.APPROVED)).thenReturn(List.of(p));
        when(transactionRepository.findByPayment(p)).thenReturn(List.of(paymentTxn(p, "100.00")));

        ReconciliationService.ReconciliationResult result = reconciliationService.reconcileApprovedPayments();

        assertThat(result.approvedCount()).isEqualTo(1);
        assertThat(result.matchedCount()).isEqualTo(1);
        assertThat(result.discrepancies()).isEmpty();
        assertThat(result.internalTotal()).isEqualByComparingTo("100.00");
        assertThat(result.ledgerTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("aprovado SEM lançamento no razão → divergência MISSING_LEDGER")
    void detectaMissingLedger() {
        Payment p = approved("PAY-2", "100.00");
        when(paymentRepository.findAllByStatus(PaymentStatus.APPROVED)).thenReturn(List.of(p));
        when(transactionRepository.findByPayment(p)).thenReturn(List.of()); // sem lançamento

        ReconciliationService.ReconciliationResult result = reconciliationService.reconcileApprovedPayments();

        assertThat(result.matchedCount()).isZero();
        assertThat(result.discrepancies()).hasSize(1);
        assertThat(result.discrepancies().get(0).type()).isEqualTo("MISSING_LEDGER");
        assertThat(result.discrepancies().get(0).referenceId()).isEqualTo("PAY-2");
    }

    @Test
    @DisplayName("lançamento com valor diferente → divergência AMOUNT_MISMATCH")
    void detectaAmountMismatch() {
        Payment p = approved("PAY-3", "100.00");
        when(paymentRepository.findAllByStatus(PaymentStatus.APPROVED)).thenReturn(List.of(p));
        when(transactionRepository.findByPayment(p)).thenReturn(List.of(paymentTxn(p, "90.00"))); // valor diverge

        ReconciliationService.ReconciliationResult result = reconciliationService.reconcileApprovedPayments();

        assertThat(result.matchedCount()).isZero();
        assertThat(result.discrepancies()).hasSize(1);
        assertThat(result.discrepancies().get(0).type()).isEqualTo("AMOUNT_MISMATCH");
    }

    @Test
    @DisplayName("mistura: 1 ok + 1 missing + 1 mismatch → 1 conciliado e 2 divergências")
    void misturaConciliadosEDivergencias() {
        Payment ok = approved("PAY-OK", "100.00");
        Payment missing = approved("PAY-MISS", "50.00");
        Payment mismatch = approved("PAY-DIFF", "200.00");
        when(paymentRepository.findAllByStatus(PaymentStatus.APPROVED)).thenReturn(List.of(ok, missing, mismatch));
        when(transactionRepository.findByPayment(ok)).thenReturn(List.of(paymentTxn(ok, "100.00")));
        when(transactionRepository.findByPayment(missing)).thenReturn(List.of());
        when(transactionRepository.findByPayment(mismatch)).thenReturn(List.of(paymentTxn(mismatch, "199.99")));

        ReconciliationService.ReconciliationResult result = reconciliationService.reconcileApprovedPayments();

        assertThat(result.approvedCount()).isEqualTo(3);
        assertThat(result.matchedCount()).isEqualTo(1);
        assertThat(result.discrepancies()).hasSize(2);
    }
}
