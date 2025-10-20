package com.zendapag.core.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all domain events in the Zendapag platform.
 * Provides common event metadata and ensures consistent event structure.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType",
    visible = true
)
@JsonSubTypes({
    // Payment Events
    @JsonSubTypes.Type(value = PaymentCreatedEvent.class, name = "payment_created"),
    @JsonSubTypes.Type(value = PaymentUpdatedEvent.class, name = "payment_updated"),
    @JsonSubTypes.Type(value = PaymentCompletedEvent.class, name = "payment_completed"),
    @JsonSubTypes.Type(value = PaymentFailedEvent.class, name = "payment_failed"),
    @JsonSubTypes.Type(value = PaymentCancelledEvent.class, name = "payment_cancelled"),
    @JsonSubTypes.Type(value = PaymentRefundedEvent.class, name = "payment_refunded"),

    // Webhook Events
    @JsonSubTypes.Type(value = WebhookTriggeredEvent.class, name = "webhook_triggered"),
    @JsonSubTypes.Type(value = WebhookDeliveredEvent.class, name = "webhook_delivered"),
    @JsonSubTypes.Type(value = WebhookFailedEvent.class, name = "webhook_failed"),
    @JsonSubTypes.Type(value = WebhookRetryEvent.class, name = "webhook_retry"),

    // Settlement Events
    @JsonSubTypes.Type(value = SettlementInitiatedEvent.class, name = "settlement_initiated"),
    @JsonSubTypes.Type(value = SettlementCompletedEvent.class, name = "settlement_completed"),
    @JsonSubTypes.Type(value = SettlementFailedEvent.class, name = "settlement_failed"),

    // Risk Events
    @JsonSubTypes.Type(value = RiskAnalysisEvent.class, name = "risk_analysis"),
    @JsonSubTypes.Type(value = FraudDetectedEvent.class, name = "fraud_detected"),
    @JsonSubTypes.Type(value = RiskScoreUpdatedEvent.class, name = "risk_score_updated"),

    // Notification Events
    @JsonSubTypes.Type(value = NotificationEvent.class, name = "notification"),
    @JsonSubTypes.Type(value = EmailSentEvent.class, name = "email_sent"),
    @JsonSubTypes.Type(value = SmsSentEvent.class, name = "sms_sent"),

    // System Events
    @JsonSubTypes.Type(value = SystemHealthEvent.class, name = "system_health"),
    @JsonSubTypes.Type(value = AuditEvent.class, name = "audit"),
})
public abstract class DomainEvent {

    @NotNull
    private final String eventId;

    @NotNull
    private final String eventType;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    private final Instant timestamp;

    private final String correlationId;
    private final String causationId;
    private final String aggregateId;
    private final String aggregateType;
    private final Long aggregateVersion;
    private final String source;
    private final Map<String, Object> metadata;

    protected DomainEvent(String eventType) {
        this(eventType, null, null, null, null, null);
    }

    protected DomainEvent(String eventType, String aggregateId, String aggregateType) {
        this(eventType, aggregateId, aggregateType, null, null, null);
    }

    protected DomainEvent(String eventType, String aggregateId, String aggregateType,
                         String correlationId, String causationId, Long aggregateVersion) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.aggregateVersion = aggregateVersion;
        this.source = "zendapag-platform";
        this.metadata = new HashMap<>();

        // Record event creation metrics
        Counter.builder("domain.events.created")
                .tag("event_type", eventType)
                .tag("aggregate_type", aggregateType != null ? aggregateType : "unknown")
                .register(Metrics.globalRegistry)
                .increment();
    }

    // Getters
    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getCausationId() {
        return causationId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public String getSource() {
        return source;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    // Metadata management
    public DomainEvent withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public DomainEvent withMetadata(Map<String, Object> additionalMetadata) {
        this.metadata.putAll(additionalMetadata);
        return this;
    }

    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }

    public String getMetadataAsString(String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }

    // Utility methods
    public boolean isOfType(String eventType) {
        return this.eventType.equals(eventType);
    }

    public boolean isOfAggregateType(String aggregateType) {
        return Objects.equals(this.aggregateType, aggregateType);
    }

    public boolean belongsToAggregate(String aggregateId) {
        return Objects.equals(this.aggregateId, aggregateId);
    }

    public boolean isPartOfFlow(String correlationId) {
        return Objects.equals(this.correlationId, correlationId);
    }

    public long getAgeInMillis() {
        return Instant.now().toEpochMilli() - timestamp.toEpochMilli();
    }

    // Abstract method to be implemented by concrete events
    public abstract Map<String, Object> getEventData();

    // Validation
    public boolean isValid() {
        return eventId != null && !eventId.isEmpty() &&
               eventType != null && !eventType.isEmpty() &&
               timestamp != null;
    }

    // For debugging and logging
    public String getEventSummary() {
        return String.format("%s[id=%s, type=%s, aggregate=%s, time=%s]",
                getClass().getSimpleName(),
                eventId,
                eventType,
                aggregateId != null ? aggregateType + ":" + aggregateId : "none",
                timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DomainEvent that = (DomainEvent) obj;
        return Objects.equals(eventId, that.eventId) &&
               Objects.equals(eventType, that.eventType) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventType, timestamp);
    }

    @Override
    public String toString() {
        return String.format("%s{eventId='%s', eventType='%s', timestamp=%s, correlationId='%s', " +
                           "aggregateId='%s', aggregateType='%s', source='%s'}",
                getClass().getSimpleName(), eventId, eventType, timestamp, correlationId,
                aggregateId, aggregateType, source);
    }
}