package com.zendapag.worker.services;

import com.zendapag.core.entity.Payment;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Análise de risco no worker — delega para a lógica REAL do core
 * (RiskService), de modo que worker e API compartilhem as mesmas regras.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAnalysisService {

    private final PaymentRepository paymentRepository;
    private final RiskService riskService;

    public RiskResult analyzePayment(UUID paymentId, BigDecimal amount, String customerDocument) {
        log.info("Analyzing risk for payment: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("Risk: pagamento {} não encontrado", paymentId);
            return new RiskResult(0, false, "Pagamento não encontrado");
        }
        RiskService.RiskAssessment assessment = riskService.assess(payment);
        boolean blocked = assessment.level() == RiskService.RiskLevel.HIGH;
        return new RiskResult(assessment.score(), blocked, String.join("; ", assessment.reasons()));
    }

    public boolean isHighRisk(UUID merchantId) {
        return false;
    }

    public record RiskResult(int score, boolean blocked, String reason) {}
}
