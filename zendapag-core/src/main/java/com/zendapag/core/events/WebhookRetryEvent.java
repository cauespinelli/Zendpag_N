package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a webhook delivery is scheduled for retry.
 */
public class WebhookRetryEvent extends DomainEvent {

    @NotNull
    private final String webhookId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final int attemptNumber;

    @NotNull
    private final int maxAttempts;

    private final String lastError;
    private final int lastHttpStatus;
    private final long nextRetryDelayMs;
    private final Instant scheduledRetryAt;

    public WebhookRetryEvent(String webhookId,
                            String merchantId,
                            int attemptNumber,
                            int maxAttempts,
                            String lastError,
                            int lastHttpStatus,
                            long nextRetryDelayMs,
                            String correlationId,
                            String causationId,
                            Long aggregateVersion) {

        super("webhook_retry", webhookId, "Webhook", correlationId, causationId, aggregateVersion);

        this.webhookId = webhookId;
        this.merchantId = merchantId;
        this.attemptNumber = attemptNumber;
        this.maxAttempts = maxAttempts;
        this.lastError = lastError;
        this.lastHttpStatus = lastHttpStatus;
        this.nextRetryDelayMs = nextRetryDelayMs;
        this.scheduledRetryAt = Instant.now().plusMillis(nextRetryDelayMs);

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("attempt", attemptNumber)
            .withMetadata("max_attempts", maxAttempts)
            .withMetadata("last_http_status", lastHttpStatus);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("webhookId", webhookId);
        data.put("merchantId", merchantId);
        data.put("attemptNumber", attemptNumber);
        data.put("maxAttempts", maxAttempts);
        data.put("lastError", lastError);
        data.put("lastHttpStatus", lastHttpStatus);
        data.put("nextRetryDelayMs", nextRetryDelayMs);
        data.put("scheduledRetryAt", scheduledRetryAt);
        return data;
    }

    public String getWebhookId() { return webhookId; }
    public String getMerchantId() { return merchantId; }
    public int getAttemptNumber() { return attemptNumber; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLastError() { return lastError; }
    public int getLastHttpStatus() { return lastHttpStatus; }
    public long getNextRetryDelayMs() { return nextRetryDelayMs; }
    public Instant getScheduledRetryAt() { return scheduledRetryAt; }

    public boolean isLastAttempt() {
        return attemptNumber >= maxAttempts;
    }

    @Override
    public String getEventSummary() {
        return String.format("WebhookRetry[id=%s, attempt=%d/%d]", webhookId, attemptNumber, maxAttempts);
    }
}
