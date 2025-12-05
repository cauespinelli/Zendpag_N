package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published for audit logging.
 */
public class AuditEvent extends DomainEvent {

    @NotNull
    private final String auditId;

    @NotNull
    private final String action;

    @NotNull
    private final String actorId;

    @NotNull
    private final String actorType;

    private final String resourceType;
    private final String resourceId;
    private final String ipAddress;
    private final String userAgent;
    private final Map<String, Object> previousState;
    private final Map<String, Object> newState;
    private final String result;
    private final Instant performedAt;
    private final String reason;

    public AuditEvent(String auditId,
                     String action,
                     String actorId,
                     String actorType,
                     String resourceType,
                     String resourceId,
                     String ipAddress,
                     String userAgent,
                     Map<String, Object> previousState,
                     Map<String, Object> newState,
                     String result,
                     String reason,
                     String correlationId) {

        super("audit", auditId, "Audit", correlationId, null, 1L);

        this.auditId = auditId;
        this.action = action;
        this.actorId = actorId;
        this.actorType = actorType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.previousState = previousState != null ? new HashMap<>(previousState) : new HashMap<>();
        this.newState = newState != null ? new HashMap<>(newState) : new HashMap<>();
        this.result = result;
        this.performedAt = Instant.now();
        this.reason = reason;

        this.withMetadata("action", action)
            .withMetadata("actor_id", actorId)
            .withMetadata("actor_type", actorType)
            .withMetadata("resource_type", resourceType)
            .withMetadata("resource_id", resourceId)
            .withMetadata("result", result);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("auditId", auditId);
        data.put("action", action);
        data.put("actorId", actorId);
        data.put("actorType", actorType);
        data.put("resourceType", resourceType);
        data.put("resourceId", resourceId);
        data.put("ipAddress", ipAddress);
        data.put("userAgent", userAgent);
        data.put("previousState", previousState);
        data.put("newState", newState);
        data.put("result", result);
        data.put("performedAt", performedAt);
        data.put("reason", reason);
        return data;
    }

    public String getAuditId() { return auditId; }
    public String getAction() { return action; }
    public String getActorId() { return actorId; }
    public String getActorType() { return actorType; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Map<String, Object> getPreviousState() { return new HashMap<>(previousState); }
    public Map<String, Object> getNewState() { return new HashMap<>(newState); }
    public String getResult() { return result; }
    public Instant getPerformedAt() { return performedAt; }
    public String getReason() { return reason; }

    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(result);
    }

    public boolean hasStateChange() {
        return !previousState.isEmpty() || !newState.isEmpty();
    }

    @Override
    public String getEventSummary() {
        return String.format("Audit[id=%s, action=%s, actor=%s, resource=%s:%s, result=%s]",
            auditId, action, actorId, resourceType, resourceId, result);
    }
}
