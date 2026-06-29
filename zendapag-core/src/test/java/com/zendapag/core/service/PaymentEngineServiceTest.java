package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.repository.PaymentRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do cálculo de taxa (MDR) e do fluxo de aprovação do motor.
 * Razão, webhook e risco são mockados para isolar a aritmética da taxa.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEngineService — taxa (MDR) e aprovação")
class PaymentEngineServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private LedgerService ledgerService;
    @Mock private WebhookService webhookService;
    @Mock private RiskService riskService;

    @InjectMocks private PaymentEngineService engine;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = new Merchant("Loja Teste", "12345678000190", "loja@teste.com");
        merchant.setId(UUID.randomUUID());
        merchant.setFeeRate(new BigDecimal("0.0199")); // 1,99%
        // Config de taxa (normalmente @Value): cartão 3,49% + 0,40%/parcela; boleto R$3,49 fixo.
        org.springframework.test.util.ReflectionTestUtils.setField(engine, "cardBaseRate", new BigDecimal("0.0349"));
        org.springframework.test.util.ReflectionTestUtils.setField(engine, "cardInstallmentSurcharge", new BigDecimal("0.0040"));
        org.springframework.test.util.ReflectionTestUtils.setField(engine, "boletoFlatFee", new BigDecimal("3.49"));
    }

    private Payment withMethod(Payment p, com.zendapag.core.entity.enums.PaymentMethodType type) {
        com.zendapag.core.entity.PaymentMethod pm = new com.zendapag.core.entity.PaymentMethod(merchant, type);
        p.setPaymentMethod(pm);
        return p;
    }

    /** Pagamento PENDING pronto para aprovação, com risco LOW por padrão. */
    private Payment pendingPayment(BigDecimal amount) {
        Payment p = new Payment("PAY-" + UUID.randomUUID().toString().substring(0, 8), merchant, amount);
        p.setId(UUID.randomUUID());
        p.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(p.getId())).thenReturn(java.util.Optional.of(p));
        when(riskService.assess(any(Payment.class)))
            .thenReturn(new RiskService.RiskAssessment(RiskService.RiskLevel.LOW, 0, List.of("Sem sinais de risco")));
        return p;
    }

    @Test
    @DisplayName("taxa normal de 1,99% sobre R$100,00 → taxa R$1,99 e líquido R$98,01")
    void taxaNormal() {
        Payment p = pendingPayment(new BigDecimal("100.00"));

        Payment result = engine.approvePayment(p.getId());

        assertThat(result.getFeeAmount()).isEqualByComparingTo("1.99");
        assertThat(result.getNetAmount()).isEqualByComparingTo("98.01");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(ledgerService).settleApprovedPayment(
            eq(p), eq(new BigDecimal("100.00")), eq(new BigDecimal("1.99")), eq(new BigDecimal("98.01")));
    }

    @Test
    @DisplayName("taxa mínima de R$0,50 quando o percentual dá menos (R$10,00 × 1,99% = R$0,20)")
    void taxaMinima() {
        Payment p = pendingPayment(new BigDecimal("10.00"));

        Payment result = engine.approvePayment(p.getId());

        // 1,99% de 10,00 = 0,199 ≈ 0,20, abaixo do mínimo → aplica R$0,50
        assertThat(result.getFeeAmount()).isEqualByComparingTo("0.50");
        assertThat(result.getNetAmount()).isEqualByComparingTo("9.50");
    }

    @Test
    @DisplayName("arredondamento HALF_UP: R$50,00 × 1,99% = 0,995 → taxa R$1,00")
    void arredondamentoHalfUp() {
        Payment p = pendingPayment(new BigDecimal("50.00"));

        Payment result = engine.approvePayment(p.getId());

        assertThat(result.getFeeAmount()).isEqualByComparingTo("1.00");
        assertThat(result.getNetAmount()).isEqualByComparingTo("49.00");
    }

    @Test
    @DisplayName("feeRate nulo no estabelecimento → usa o default de 1,99%")
    void feeRateNuloUsaDefault() {
        merchant.setFeeRate(null);
        Payment p = pendingPayment(new BigDecimal("100.00"));

        Payment result = engine.approvePayment(p.getId());

        assertThat(result.getFeeAmount()).isEqualByComparingTo("1.99");
        assertThat(result.getNetAmount()).isEqualByComparingTo("98.01");
    }

    @Test
    @DisplayName("valor alto R$1.000.000,00 × 1,99% → taxa R$19.900,00, líquido R$980.100,00")
    void valorAlto() {
        Payment p = pendingPayment(new BigDecimal("1000000.00"));

        Payment result = engine.approvePayment(p.getId());

        assertThat(result.getFeeAmount()).isEqualByComparingTo("19900.00");
        assertThat(result.getNetAmount()).isEqualByComparingTo("980100.00");
    }

    @Test
    @DisplayName("BORDA R$0,01: a taxa é limitada ao bruto e o líquido fica em ZERO (nunca negativo)")
    void bordaUmCentavoLiquidoNaoNegativo() {
        // Correção do achado #1: a taxa nunca excede o bruto. Para R$0,01, a taxa
        // mínima de R$0,50 seria maior que o bruto, então é limitada a R$0,01 e o
        // líquido fica em zero — não mais negativo.
        Payment p = pendingPayment(new BigDecimal("0.01"));

        Payment result = engine.approvePayment(p.getId());

        assertThat(result.getFeeAmount()).isEqualByComparingTo("0.01");
        assertThat(result.getNetAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("BORDA: bruto igual à taxa mínima (R$0,50) → líquido zero, taxa R$0,50")
    void bordaIgualTaxaMinima() {
        Payment p = pendingPayment(new BigDecimal("0.50"));

        Payment result = engine.approvePayment(p.getId());

        assertThat(result.getFeeAmount()).isEqualByComparingTo("0.50");
        assertThat(result.getNetAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("idempotência: aprovar um pagamento já APPROVED lança e NÃO credita de novo")
    void reaprovarNaoCreditaDeNovo() {
        Payment p = new Payment("PAY-DUP", merchant, new BigDecimal("100.00"));
        p.setId(UUID.randomUUID());
        p.setStatus(PaymentStatus.APPROVED); // já aprovado
        when(paymentRepository.findById(p.getId())).thenReturn(java.util.Optional.of(p));

        assertThatThrownBy(() -> engine.approvePayment(p.getId()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("PENDING");

        verify(ledgerService, never()).settleApprovedPayment(any(), any(), any(), any());
        verify(webhookService, never()).notifyMerchant(any(), any(), any());
    }

    @Test
    @DisplayName("risco HIGH retém o pagamento: lança e NÃO credita nem notifica")
    void riscoAltoRetem() {
        Payment p = new Payment("PAY-RISK", merchant, new BigDecimal("100.00"));
        p.setId(UUID.randomUUID());
        p.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(p.getId())).thenReturn(java.util.Optional.of(p));
        when(riskService.assess(any(Payment.class)))
            .thenReturn(new RiskService.RiskAssessment(RiskService.RiskLevel.HIGH, 80, List.of("Valor alto")));

        assertThatThrownBy(() -> engine.approvePayment(p.getId()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("risco alto");

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING); // permanece retido
        verify(ledgerService, never()).settleApprovedPayment(any(), any(), any(), any());
        verify(webhookService, never()).notifyMerchant(any(), any(), any());
    }

    @Test
    @DisplayName("CARTÃO 1x: taxa base 3,49% sobre R$200,00 → R$6,98 (sem acréscimo de parcela)")
    void cartaoAVista() {
        Payment p = withMethod(pendingPayment(new BigDecimal("200.00")), com.zendapag.core.entity.enums.PaymentMethodType.CREDIT_CARD);
        p.setInstallments(1);

        Payment result = engine.approvePayment(p.getId());

        // 200 * 3,49% = 6,98
        assertThat(result.getFeeAmount()).isEqualByComparingTo("6.98");
        assertThat(result.getNetAmount()).isEqualByComparingTo("193.02");
    }

    @Test
    @DisplayName("CARTÃO 3x: taxa por parcela = 3,49% + 2×0,40% = 4,29% sobre R$300 → R$12,87")
    void cartaoParcelado() {
        Payment p = withMethod(pendingPayment(new BigDecimal("300.00")), com.zendapag.core.entity.enums.PaymentMethodType.CREDIT_CARD);
        p.setInstallments(3);

        Payment result = engine.approvePayment(p.getId());

        // 300 * (0,0349 + 2*0,0040) = 300 * 0,0429 = 12,87
        assertThat(result.getFeeAmount()).isEqualByComparingTo("12.87");
        assertThat(result.getNetAmount()).isEqualByComparingTo("287.13");
    }

    @Test
    @DisplayName("BOLETO: tarifa FIXA de R$3,49 (não percentual) sobre R$250 → líquido R$246,51")
    void boletoTarifaFixa() {
        Payment p = withMethod(pendingPayment(new BigDecimal("250.00")), com.zendapag.core.entity.enums.PaymentMethodType.BANK_SLIP);

        Payment result = engine.approvePayment(p.getId());

        assertThat(result.getFeeAmount()).isEqualByComparingTo("3.49");
        assertThat(result.getNetAmount()).isEqualByComparingTo("246.51");
        assertThat(result.getFeeRate()).isNull(); // tarifa fixa, sem percentual
    }
}
