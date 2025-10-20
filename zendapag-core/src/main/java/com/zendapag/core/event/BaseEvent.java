package com.zendapag.core.event;

import com.zendapag.core.entity.Merchant;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseEvent {
    private final UUID eventId;
    private final Instant timestamp;
    private final String eventType;
    private final Merchant merchant;
    private final String correlationId;

    protected BaseEvent(String eventType, Merchant merchant, String correlationId) {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.eventType = eventType;
        this.merchant = merchant;
        this.correlationId = correlationId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}