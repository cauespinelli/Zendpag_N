package com.zendapag.worker.services;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class RiskAnalysisService {

    public RiskResult analyzePayment(UUID paymentId, BigDecimal amount, String customerDocument) {
        log.info("Analyzing risk for payment: {}", paymentId);
        // TODO: Implement actual risk analysis
        return new RiskResult(0, false, "OK");
    }

    public boolean isHighRisk(UUID merchantId) {
        return false;
    }

    public record RiskResult(int score, boolean blocked, String reason) {}
}
