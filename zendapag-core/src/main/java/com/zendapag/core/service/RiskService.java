package com.zendapag.core.service;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Avaliação de risco/antifraude (regras reais, determinísticas). Produz um
 * score 0–100, um nível (LOW/MEDIUM/HIGH) e os motivos. Usado pelo motor na
 * aprovação do pagamento (HIGH retém) e pelo worker (staging/prod).
 *
 * Sandbox: regras simples e auditáveis (valor, KYC, velocidade). Em produção,
 * entrariam device fingerprint, listas de bloqueio, modelos etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskService {

    private static final BigDecimal HIGH_AMOUNT = new BigDecimal("10000");
    private static final BigDecimal VERY_HIGH_AMOUNT = new BigDecimal("50000");
    private static final long VELOCITY_LIMIT = 50; // pagamentos/dia por estabelecimento

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public RiskAssessment assess(Payment payment) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        Merchant merchant = payment.getMerchant();

        if (amount.compareTo(HIGH_AMOUNT) > 0) {
            score += 40;
            reasons.add("Valor alto (> R$ 10.000)");
        }
        if (amount.compareTo(VERY_HIGH_AMOUNT) > 0) {
            score += 30;
            reasons.add("Valor muito alto (> R$ 50.000)");
        }
        if (merchant != null && !Boolean.TRUE.equals(merchant.getKycVerified())) {
            score += 30;
            reasons.add("Estabelecimento sem KYC verificado");
        }
        if (merchant != null) {
            try {
                java.time.Instant startOfDay = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
                long todayCount = paymentRepository.countTodayPaymentsByMerchant(merchant, startOfDay);
                if (todayCount > VELOCITY_LIMIT) {
                    score += 20;
                    reasons.add("Velocidade alta (" + todayCount + " pagamentos hoje)");
                }
            } catch (Exception e) {
                log.debug("Falha ao avaliar velocidade: {}", e.getMessage());
            }
        }

        score = Math.min(score, 100);
        RiskLevel level = score >= 70 ? RiskLevel.HIGH : (score >= 40 ? RiskLevel.MEDIUM : RiskLevel.LOW);
        if (reasons.isEmpty()) {
            reasons.add("Sem sinais de risco");
        }
        return new RiskAssessment(level, score, reasons);
    }

    /** Compatibilidade com a assinatura antiga (retorno em String). */
    public String assessRisk(Payment payment) {
        return assess(payment).level().name();
    }

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    public record RiskAssessment(RiskLevel level, int score, List<String> reasons) {}
}
