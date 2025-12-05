package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a webhook is triggered.
 */
public class WebhookTriggeredEvent extends DomainEvent {

    @NotNull
    private final String webhookId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final String eventType;

    @NotNull
    private final String targetUrl;

    private final String payload;
    private final Instant triggeredAt;
    private final Map<String, String> headers;

    public WebhookTriggeredEvent(String webhookId,
                                String merchantId,
                                String eventType,
                                String targetUrl,
                                String payload,
                                Map<String, String> headers,
                                String correlationId) {

        super("webhook_triggered", webhookId, "Webhook", correlationId, null, 1L);

        this.webhookId = webhookId;
        this.merchantId = merchantId;
        this.eventType = eventType;
        this.targetUrl = targetUrl;
        this.payload = payload;
        this.triggeredAt = Instant.now();
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("event_type", eventType)
            .withMetadata("target_url", targetUrl);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("webhookId", webhookId);
        data.put("merchantId", merchantId);
        data.put("eventType", eventType);
        data.put("targetUrl", targetUrl);
        data.put("payload", payload);
        data.put("triggeredAt", triggeredAt);
        data.put("headers", headers);
        return data;
    }

    public String getWebhookId() { return webhookId; }
    public String getMerchantId() { return merchantId; }
    public String getEventType() { return eventType; }
    public String getTargetUrl() { return targetUrl; }
    public String getPayload() { return payload; }
    public Instant getTriggeredAt() { return triggeredAt; }
    public Map<String, String> getHeaders() { return new HashMap<>(headers); }

    @Override
    public String getEventSummary() {
        return String.format("WebhookTriggered[id=%s, event=%s, url=%s]", webhookId, eventType, targetUrl);
    }
}
