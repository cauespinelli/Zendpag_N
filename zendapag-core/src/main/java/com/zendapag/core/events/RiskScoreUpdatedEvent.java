package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event published when a risk score is updated.
 */
public class RiskScoreUpdatedEvent extends DomainEvent {

    @NotNull
    private final String entityId;

    @NotNull
    private final String entityType;

    @NotNull
    private final int previousScore;

    @NotNull
    private final int newScore;

    private final String previousLevel;
    private final String newLevel;
    private final String updateReason;
    private final List<String> contributingFactors;
    private final Instant updatedAt;

    public RiskScoreUpdatedEvent(String entityId,
                                String entityType,
                                int previousScore,
                                int newScore,
                                String previousLevel,
                                String newLevel,
                                String updateReason,
                                List<String> contributingFactors,
                                String correlationId) {

        super("risk_score_updated", entityId, "Risk", correlationId, null, 1L);

        this.entityId = entityId;
        this.entityType = entityType;
        this.previousScore = previousScore;
        this.newScore = newScore;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.updateReason = updateReason;
        this.contributingFactors = contributingFactors;
        this.updatedAt = Instant.now();

        this.withMetadata("entity_id", entityId)
            .withMetadata("entity_type", entityType)
            .withMetadata("previous_score", previousScore)
            .withMetadata("new_score", newScore)
            .withMetadata("score_change", newScore - previousScore);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("entityId", entityId);
        data.put("entityType", entityType);
        data.put("previousScore", previousScore);
        data.put("newScore", newScore);
        data.put("previousLevel", previousLevel);
        data.put("newLevel", newLevel);
        data.put("updateReason", updateReason);
        data.put("contributingFactors", contributingFactors);
        data.put("updatedAt", updatedAt);
        return data;
    }

    public String getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public int getPreviousScore() { return previousScore; }
    public int getNewScore() { return newScore; }
    public String getPreviousLevel() { return previousLevel; }
    public String getNewLevel() { return newLevel; }
    public String getUpdateReason() { return updateReason; }
    public List<String> getContributingFactors() { return contributingFactors; }
    public Instant getUpdatedAt() { return updatedAt; }

    public int getScoreChange() {
        return newScore - previousScore;
    }

    public boolean hasLevelChanged() {
        return !newLevel.equals(previousLevel);
    }

    public boolean isEscalation() {
        return newScore > previousScore;
    }

    @Override
    public String getEventSummary() {
        return String.format("RiskScoreUpdated[entity=%s, %d -> %d, level=%s]",
            entityId, previousScore, newScore, newLevel);
    }
}
