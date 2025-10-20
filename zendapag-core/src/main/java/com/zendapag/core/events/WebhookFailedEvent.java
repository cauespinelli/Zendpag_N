package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a webhook delivery fails.
 * This triggers retry mechanisms and dead letter queue processing.
 */
public class WebhookFailedEvent extends DomainEvent {

    @NotNull
    private final String webhookId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final String deliveryId;

    @NotNull
    private final String originalEventType;

    @NotNull
    private final String originalEventId;

    @NotNull
    private final String webhookUrl;

    @NotNull
    private final Instant failedAt;

    @Positive
    private final int attemptNumber;

    @Positive
    private final int maxAttempts;

    private final int httpStatusCode;
    private final String errorMessage;
    private final String responseBody;
    private final long responseTimeMs;
    private final Instant nextRetryAt;
    private final String failureReason;
    private final Map<String, String> requestHeaders;
    private final Map<String, String> responseHeaders;
    private final String requestPayload;

    public WebhookFailedEvent(String webhookId,
                            String merchantId,
                            String deliveryId,
                            String originalEventType,
                            String originalEventId,
                            String webhookUrl,
                            Instant failedAt,
                            int attemptNumber,
                            int maxAttempts,
                            int httpStatusCode,
                            String errorMessage,
                            String responseBody,
                            long responseTimeMs,
                            Instant nextRetryAt,
                            String failureReason,
                            Map<String, String> requestHeaders,
                            Map<String, String> responseHeaders,
                            String requestPayload,
                            String correlationId,
                            String causationId) {

        super("webhook_failed", webhookId, "Webhook", correlationId, causationId, null);

        this.webhookId = webhookId;
        this.merchantId = merchantId;
        this.deliveryId = deliveryId;
        this.originalEventType = originalEventType;
        this.originalEventId = originalEventId;
        this.webhookUrl = webhookUrl;
        this.failedAt = failedAt;
        this.attemptNumber = attemptNumber;
        this.maxAttempts = maxAttempts;
        this.httpStatusCode = httpStatusCode;
        this.errorMessage = errorMessage;
        this.responseBody = responseBody;
        this.responseTimeMs = responseTimeMs;
        this.nextRetryAt = nextRetryAt;
        this.failureReason = failureReason;
        this.requestHeaders = requestHeaders != null ? new HashMap<>(requestHeaders) : new HashMap<>();
        this.responseHeaders = responseHeaders != null ? new HashMap<>(responseHeaders) : new HashMap<>();
        this.requestPayload = requestPayload;

        // Add contextual metadata
        this.withMetadata("merchant_id", merchantId)
            .withMetadata("webhook_url", webhookUrl)
            .withMetadata("original_event_type", originalEventType)
            .withMetadata("attempt_number", attemptNumber)
            .withMetadata("max_attempts", maxAttempts)
            .withMetadata("http_status", httpStatusCode)
            .withMetadata("response_time_ms", responseTimeMs)
            .withMetadata("is_final_attempt", isLastAttempt())
            .withMetadata("will_retry", willRetry())
            .withMetadata("failure_category", categorizeFailure());
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("webhookId", webhookId);
        data.put("merchantId", merchantId);
        data.put("deliveryId", deliveryId);
        data.put("originalEventType", originalEventType);
        data.put("originalEventId", originalEventId);
        data.put("webhookUrl", webhookUrl);
        data.put("failedAt", failedAt);
        data.put("attemptNumber", attemptNumber);
        data.put("maxAttempts", maxAttempts);
        data.put("httpStatusCode", httpStatusCode);
        data.put("errorMessage", errorMessage);
        data.put("responseBody", responseBody);
        data.put("responseTimeMs", responseTimeMs);
        data.put("nextRetryAt", nextRetryAt);
        data.put("failureReason", failureReason);
        data.put("requestHeaders", requestHeaders);
        data.put("responseHeaders", responseHeaders);
        data.put("requestPayload", requestPayload);
        return data;
    }

    // Getters
    public String getWebhookId() {
        return webhookId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public String getOriginalEventType() {
        return originalEventType;
    }

    public String getOriginalEventId() {
        return originalEventId;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Map<String, String> getRequestHeaders() {
        return new HashMap<>(requestHeaders);
    }

    public Map<String, String> getResponseHeaders() {
        return new HashMap<>(responseHeaders);
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    // Business logic methods
    public boolean isLastAttempt() {
        return attemptNumber >= maxAttempts;
    }

    public boolean willRetry() {
        return !isLastAttempt() && isRetryable();
    }

    public boolean shouldGoToDeadLetterQueue() {
        return isLastAttempt() || !isRetryable();
    }

    public boolean isRetryable() {
        // Don't retry for client errors (4xx) except for specific cases
        if (httpStatusCode >= 400 && httpStatusCode < 500) {
            // Retry for rate limiting and temporary client issues
            return httpStatusCode == 408 || // Request Timeout
                   httpStatusCode == 429 || // Too Many Requests
                   httpStatusCode == 499;   // Client Closed Request
        }

        // Retry for server errors (5xx) and network errors (0)
        return httpStatusCode == 0 || (httpStatusCode >= 500 && httpStatusCode < 600);
    }

    public String categorizeFailure() {
        if (httpStatusCode == 0) {
            return "network_error";
        } else if (httpStatusCode >= 400 && httpStatusCode < 500) {
            return "client_error";
        } else if (httpStatusCode >= 500 && httpStatusCode < 600) {
            return "server_error";
        } else if (responseTimeMs > 30000) { // 30 seconds timeout
            return "timeout";
        } else {
            return "unknown";
        }
    }

    public boolean isNetworkError() {
        return httpStatusCode == 0 || "network_error".equals(categorizeFailure());
    }

    public boolean isClientError() {
        return httpStatusCode >= 400 && httpStatusCode < 500;
    }

    public boolean isServerError() {
        return httpStatusCode >= 500 && httpStatusCode < 600;
    }

    public boolean isTimeout() {
        return responseTimeMs > 30000;
    }

    public boolean isRateLimited() {
        return httpStatusCode == 429;
    }

    public long getTimeSinceFailure() {
        return Instant.now().toEpochMilli() - failedAt.toEpochMilli();
    }

    public long getTimeUntilRetry() {
        if (nextRetryAt == null) {
            return -1;
        }
        return nextRetryAt.toEpochMilli() - Instant.now().toEpochMilli();
    }

    public int getRemainingAttempts() {
        return Math.max(0, maxAttempts - attemptNumber);
    }

    // Validation methods
    public boolean hasRequiredFields() {
        return webhookId != null &&
               merchantId != null &&
               deliveryId != null &&
               originalEventType != null &&
               originalEventId != null &&
               webhookUrl != null &&
               failedAt != null &&
               attemptNumber > 0 &&
               maxAttempts > 0;
    }

    public boolean hasValidStatusCode() {
        return httpStatusCode >= 0 && httpStatusCode <= 999;
    }

    public boolean hasValidResponseTime() {
        return responseTimeMs >= 0;
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
               hasRequiredFields() &&
               hasValidStatusCode() &&
               hasValidResponseTime();
    }

    public String getRequestHeader(String name) {
        return requestHeaders.get(name);
    }

    public String getResponseHeader(String name) {
        return responseHeaders.get(name);
    }

    @Override
    public String getEventSummary() {
        return String.format("WebhookFailed[id=%s, merchant=%s, url=%s, attempt=%d/%d, status=%d, reason=%s]",
                webhookId, merchantId, webhookUrl, attemptNumber, maxAttempts, httpStatusCode, failureReason);
    }
}