package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event published when a risk analysis is performed.
 */
public class RiskAnalysisEvent extends DomainEvent {

    @NotNull
    private final String analysisId;

    @NotNull
    private final String entityId;

    @NotNull
    private final String entityType;

    @NotNull
    private final int riskScore;

    @NotNull
    private final String riskLevel;

    private final List<String> riskFactors;
    private final String recommendation;
    private final BigDecimal transactionAmount;
    private final Instant analyzedAt;
    private final long analysisTimeMs;

    public RiskAnalysisEvent(String analysisId,
                            String entityId,
                            String entityType,
                            int riskScore,
                            String riskLevel,
                            List<String> riskFactors,
                            String recommendation,
                            BigDecimal transactionAmount,
                            long analysisTimeMs,
                            String correlationId) {

        super("risk_analysis", analysisId, "Risk", correlationId, null, 1L);

        this.analysisId = analysisId;
        this.entityId = entityId;
        this.entityType = entityType;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.riskFactors = riskFactors;
        this.recommendation = recommendation;
        this.transactionAmount = transactionAmount;
        this.analyzedAt = Instant.now();
        this.analysisTimeMs = analysisTimeMs;

        this.withMetadata("entity_id", entityId)
            .withMetadata("entity_type", entityType)
            .withMetadata("risk_score", riskScore)
            .withMetadata("risk_level", riskLevel);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("analysisId", analysisId);
        data.put("entityId", entityId);
        data.put("entityType", entityType);
        data.put("riskScore", riskScore);
        data.put("riskLevel", riskLevel);
        data.put("riskFactors", riskFactors);
        data.put("recommendation", recommendation);
        data.put("transactionAmount", transactionAmount);
        data.put("analyzedAt", analyzedAt);
        data.put("analysisTimeMs", analysisTimeMs);
        return data;
    }

    public String getAnalysisId() { return analysisId; }
    public String getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public int getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public List<String> getRiskFactors() { return riskFactors; }
    public String getRecommendation() { return recommendation; }
    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public long getAnalysisTimeMs() { return analysisTimeMs; }

    public boolean isHighRisk() {
        return riskScore >= 70;
    }

    @Override
    public String getEventSummary() {
        return String.format("RiskAnalysis[id=%s, entity=%s, score=%d, level=%s]",
            analysisId, entityId, riskScore, riskLevel);
    }
}
