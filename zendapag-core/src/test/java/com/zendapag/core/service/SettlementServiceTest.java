package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Settlement;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.SettlementStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.SettlementRepository;
import com.zendapag.core.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes da liquidação (repasse): agrupamento de transações não-liquidadas,
 * cálculo de totais e transição de status PENDING → PROCESSING → SETTLED.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementService — liquidação (repasse)")
class SettlementServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SettlementRepository settlementRepository;

    @InjectMocks private SettlementService settlementService;

    private Merchant merchant;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        merchant = new Merchant("Loja Teste", "12345678000190", "loja@teste.com");
        merchant.setId(merchantId);
    }

    private Transaction paymentTxn(String ref, String gross, String fee, String net) {
        Transaction t = new Transaction(ref, merchant, TransactionType.PAYMENT, new BigDecimal(gross));
        t.setFeeAmount(new BigDecimal(fee));
        t.setNetAmount(new BigDecimal(net));
        return t;
    }

    @Test
    @DisplayName("agrupa as transações não-liquidadas e soma bruto/taxa/líquido")
    void agrupaESomaTotais() {
        List<Transaction> pending = List.of(
            paymentTxn("TXN-1", "100.00", "1.99", "98.01"),
            paymentTxn("TXN-2", "200.00", "3.98", "196.02"),
            paymentTxn("TXN-3", "50.00", "1.00", "49.00"));
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.findUnsettledPaymentsByMerchant(merchant)).thenReturn(pending);
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> inv.getArgument(0));

        Settlement result = settlementService.settleMerchant(merchantId);

        assertThat(result.getGrossAmount()).isEqualByComparingTo("350.00");
        assertThat(result.getFeeAmount()).isEqualByComparingTo("6.97");
        assertThat(result.getNetAmount()).isEqualByComparingTo("343.03");
        verify(transactionRepository).saveAll(pending); // vínculo settlement_id persistido
    }

    @Test
    @DisplayName("transição de status PENDING → PROCESSING → SETTLED (lote fica SETTLED)")
    void transicaoDeStatusAteSettled() {
        List<Transaction> pending = List.of(paymentTxn("TXN-1", "100.00", "1.99", "98.01"));
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.findUnsettledPaymentsByMerchant(merchant)).thenReturn(pending);
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> inv.getArgument(0));

        Settlement result = settlementService.settleMerchant(merchantId);

        // settle() só é possível a partir de PROCESSING, que só é possível a partir de PENDING:
        // chegar a SETTLED prova que a cadeia inteira ocorreu.
        assertThat(result.getStatus()).isEqualTo(SettlementStatus.SETTLED);
    }

    @Test
    @DisplayName("lote vazio: lança BusinessException limpa (sem NPE)")
    void loteVazioLancaErroLimpo() {
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.findUnsettledPaymentsByMerchant(merchant)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> settlementService.settleMerchant(merchantId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Nenhum pagamento pendente");
    }
}
