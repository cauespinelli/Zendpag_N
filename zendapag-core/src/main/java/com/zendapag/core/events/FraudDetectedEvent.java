package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event published when fraud is detected.
 */
public class FraudDetectedEvent extends DomainEvent {

    @NotNull
    private final String fraudId;

    @NotNull
    private final String transactionId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final String fraudType;

    @NotNull
    private final int confidenceScore;

    private final BigDecimal amount;
    private final List<String> indicators;
    private final String action;
    private final Instant detectedAt;
    private final String analysisDetails;

    public FraudDetectedEvent(String fraudId,
                             String transactionId,
                             String merchantId,
                             String fraudType,
                             int confidenceScore,
                             BigDecimal amount,
                             List<String> indicators,
                             String action,
                             String analysisDetails,
                             String correlationId) {

        super("fraud_detected", fraudId, "Fraud", correlationId, null, 1L);

        this.fraudId = fraudId;
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.fraudType = fraudType;
        this.confidenceScore = confidenceScore;
        this.amount = amount;
        this.indicators = indicators;
        this.action = action;
        this.detectedAt = Instant.now();
        this.analysisDetails = analysisDetails;

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("transaction_id", transactionId)
            .withMetadata("fraud_type", fraudType)
            .withMetadata("confidence_score", confidenceScore)
            .withMetadata("action", action);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("fraudId", fraudId);
        data.put("transactionId", transactionId);
        data.put("merchantId", merchantId);
        data.put("fraudType", fraudType);
        data.put("confidenceScore", confidenceScore);
        data.put("amount", amount);
        data.put("indicators", indicators);
        data.put("action", action);
        data.put("detectedAt", detectedAt);
        data.put("analysisDetails", analysisDetails);
        return data;
    }

    public String getFraudId() { return fraudId; }
    public String getTransactionId() { return transactionId; }
    public String getMerchantId() { return merchantId; }
    public String getFraudType() { return fraudType; }
    public int getConfidenceScore() { return confidenceScore; }
    public BigDecimal getAmount() { return amount; }
    public List<String> getIndicators() { return indicators; }
    public String getAction() { return action; }
    public Instant getDetectedAt() { return detectedAt; }
    public String getAnalysisDetails() { return analysisDetails; }

    public boolean requiresManualReview() {
        return confidenceScore >= 50 && confidenceScore < 80;
    }

    public boolean shouldBlock() {
        return confidenceScore >= 80;
    }

    @Override
    public String getEventSummary() {
        return String.format("FraudDetected[id=%s, type=%s, confidence=%d, action=%s]",
            fraudId, fraudType, confidenceScore, action);
    }
}
