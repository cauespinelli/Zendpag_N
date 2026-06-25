package com.zendapag.core.service;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes das regras de risco/antifraude. O peso de cada fator (valor, KYC,
 * velocidade) é determinístico, então assertamos score e nível exatos.
 *
 * Pesos atuais: > R$10k = +40, > R$50k = +30 (acumula com o de 10k),
 * sem KYC = +30, velocidade > 50/dia = +20. HIGH ≥ 70, MEDIUM ≥ 40, senão LOW.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskService — antifraude")
class RiskServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @InjectMocks private RiskService riskService;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = new Merchant("Loja Teste", "12345678000190", "loja@teste.com");
        merchant.setId(UUID.randomUUID());
    }

    private Payment payment(String amount) {
        return new Payment("PAY-001", merchant, new BigDecimal(amount));
    }

    private void stubVelocity(long count) {
        when(paymentRepository.countTodayPaymentsByMerchant(eq(merchant), any(Instant.class))).thenReturn(count);
    }

    @Test
    @DisplayName("LOW: valor baixo + KYC ok + velocidade ok → score 0, aprova")
    void scoreBaixoAprova() {
        merchant.setKycVerified(true);
        stubVelocity(0);

        RiskService.RiskAssessment a = riskService.assess(payment("100.00"));

        assertThat(a.score()).isEqualTo(0);
        assertThat(a.level()).isEqualTo(RiskService.RiskLevel.LOW);
    }

    @Test
    @DisplayName("HIGH: valor > R$10k (+40) + sem KYC (+30) = 70 → retém")
    void valorAltoSemKycRetem() {
        merchant.setKycVerified(false);
        stubVelocity(0);

        RiskService.RiskAssessment a = riskService.assess(payment("15000.00"));

        assertThat(a.score()).isEqualTo(70);
        assertThat(a.level()).isEqualTo(RiskService.RiskLevel.HIGH);
    }

    @Test
    @DisplayName("HIGH: valor > R$50k acumula os dois pesos (40+30=70), mesmo com KYC")
    void valorMuitoAltoEhHigh() {
        merchant.setKycVerified(true);
        stubVelocity(0);

        RiskService.RiskAssessment a = riskService.assess(payment("60000.00"));

        assertThat(a.score()).isEqualTo(70);
        assertThat(a.level()).isEqualTo(RiskService.RiskLevel.HIGH);
    }

    @Test
    @DisplayName("fator KYC: sem KYC sozinho = 30 → ainda LOW (abaixo do limiar 40)")
    void semKycSozinhoEhLow() {
        merchant.setKycVerified(false);
        stubVelocity(0);

        RiskService.RiskAssessment a = riskService.assess(payment("100.00"));

        assertThat(a.score()).isEqualTo(30);
        assertThat(a.level()).isEqualTo(RiskService.RiskLevel.LOW);
    }

    @Test
    @DisplayName("fator velocidade: valor alto (+40) + velocidade > 50/dia (+20) = 60 → MEDIUM")
    void velocidadeAltaSomaScore() {
        merchant.setKycVerified(true);
        stubVelocity(51);

        RiskService.RiskAssessment a = riskService.assess(payment("15000.00"));

        assertThat(a.score()).isEqualTo(60);
        assertThat(a.level()).isEqualTo(RiskService.RiskLevel.MEDIUM);
    }

    @Test
    @DisplayName("score é limitado a 100 mesmo com todos os fatores (40+30+30+20=120 → 100)")
    void scoreLimitadoA100() {
        merchant.setKycVerified(false);
        stubVelocity(51);

        RiskService.RiskAssessment a = riskService.assess(payment("60000.00"));

        assertThat(a.score()).isEqualTo(100);
        assertThat(a.level()).isEqualTo(RiskService.RiskLevel.HIGH);
    }
}
