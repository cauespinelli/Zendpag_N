package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a webhook is successfully delivered.
 */
public class WebhookDeliveredEvent extends DomainEvent {

    @NotNull
    private final String webhookId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final int httpStatusCode;

    private final long responseTimeMs;
    private final String responseBody;
    private final Instant deliveredAt;
    private final int attemptNumber;

    public WebhookDeliveredEvent(String webhookId,
                                String merchantId,
                                int httpStatusCode,
                                long responseTimeMs,
                                String responseBody,
                                int attemptNumber,
                                String correlationId,
                                String causationId,
                                Long aggregateVersion) {

        super("webhook_delivered", webhookId, "Webhook", correlationId, causationId, aggregateVersion);

        this.webhookId = webhookId;
        this.merchantId = merchantId;
        this.httpStatusCode = httpStatusCode;
        this.responseTimeMs = responseTimeMs;
        this.responseBody = responseBody;
        this.deliveredAt = Instant.now();
        this.attemptNumber = attemptNumber;

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("http_status", httpStatusCode)
            .withMetadata("response_time_ms", responseTimeMs)
            .withMetadata("attempt", attemptNumber);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("webhookId", webhookId);
        data.put("merchantId", merchantId);
        data.put("httpStatusCode", httpStatusCode);
        data.put("responseTimeMs", responseTimeMs);
        data.put("responseBody", responseBody);
        data.put("deliveredAt", deliveredAt);
        data.put("attemptNumber", attemptNumber);
        return data;
    }

    public String getWebhookId() { return webhookId; }
    public String getMerchantId() { return merchantId; }
    public int getHttpStatusCode() { return httpStatusCode; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public String getResponseBody() { return responseBody; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public int getAttemptNumber() { return attemptNumber; }

    @Override
    public String getEventSummary() {
        return String.format("WebhookDelivered[id=%s, status=%d, time=%dms]", webhookId, httpStatusCode, responseTimeMs);
    }
}
