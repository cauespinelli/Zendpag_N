package com.zendapag.core.event.risk;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.event.BaseEvent;

import java.util.Map;

public class RiskEvaluatedEvent extends BaseEvent {
    private final String entityType;
    private final String entityId;
    private final Integer riskScore;
    private final String riskLevel;
    private final Map<String, Object> riskFactors;

    public RiskEvaluatedEvent(Merchant merchant, String entityType, String entityId,
                             Integer riskScore, String riskLevel, Map<String, Object> riskFactors,
                             String correlationId) {
        super("risk.evaluated", merchant, correlationId);
        this.entityType = entityType;
        this.entityId = entityId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.riskFactors = riskFactors;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public Map<String, Object> getRiskFactors() {
        return riskFactors;
    }
}