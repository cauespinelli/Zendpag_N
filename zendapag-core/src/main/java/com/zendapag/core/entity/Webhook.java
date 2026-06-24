package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.WebhookStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "webhooks", indexes = {
    @Index(name = "idx_webhook_merchant", columnList = "merchant_id"),
    @Index(name = "idx_webhook_payment", columnList = "payment_id"),
    @Index(name = "idx_webhook_status", columnList = "status"),
    @Index(name = "idx_webhook_event_type", columnList = "event_type"),
    @Index(name = "idx_webhook_url", columnList = "url"),
    @Index(name = "idx_webhook_scheduled_at", columnList = "scheduled_at"),
    @Index(name = "idx_webhook_sent_at", columnList = "sent_at"),
    @Index(name = "idx_webhook_next_retry", columnList = "next_retry_at"),
    @Index(name = "idx_webhook_created_at", columnList = "created_at"),
    @Index(name = "idx_webhook_deleted", columnList = "deleted")
})
@SQLDelete(sql = "UPDATE webhooks SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Webhook extends BaseEntity {

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_webhook_merchant"))
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", foreignKey = @ForeignKey(name = "fk_webhook_payment"))
    private Payment payment;

    @NotBlank(message = "Event type is required")
    @Size(max = 100, message = "Event type must be at most 100 characters")
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WebhookStatus status;

    @NotBlank(message = "URL is required")
    @Size(max = 500, message = "URL must be at most 500 characters")
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @NotBlank(message = "HTTP method is required")
    @Size(max = 10, message = "HTTP method must be at most 10 characters")
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod = "POST";

    // Request Information
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private Map<String, Object> payload = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers")
    private Map<String, String> headers = new HashMap<>();

    @Size(max = 255, message = "Signature must be at most 255 characters")
    @Column(name = "signature")
    private String signature;

    // Response Information
    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_headers")
    private Map<String, String> responseHeaders = new HashMap<>();

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    // Timing
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    // Retry Logic
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 5;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "retry_delay_seconds", nullable = false)
    private Integer retryDelaySeconds = 60;

    // Error Information
    @Size(max = 1000, message = "Error message must be at most 1000 characters")
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Size(max = 100, message = "Error code must be at most 100 characters")
    @Column(name = "error_code", length = 100)
    private String errorCode;

    // Metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata = new HashMap<>();

    // External References
    @Size(max = 255, message = "External ID must be at most 255 characters")
    @Column(name = "external_id")
    private String externalId;

    // Correlation and Entity tracking
    @Size(max = 255, message = "Correlation ID must be at most 255 characters")
    @Column(name = "correlation_id")
    private String correlationId;

    @Size(max = 255, message = "Entity ID must be at most 255 characters")
    @Column(name = "entity_id")
    private String entityId;

    // Additional response tracking
    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "response_time")
    private Long responseTime;

    // Tags for filtering
    @Size(max = 1000, message = "Tags must be at most 1000 characters")
    @Column(name = "tags", length = 1000)
    private String tags;

    // Constructors
    public Webhook() {
        super();
        this.status = WebhookStatus.PENDING;
        this.httpMethod = "POST";
        this.retryCount = 0;
        this.maxRetries = 5;
        this.retryDelaySeconds = 60;
        this.scheduledAt = Instant.now();
    }

    public Webhook(Merchant merchant, String eventType, String url, Map<String, Object> payload) {
        this();
        this.merchant = merchant;
        this.eventType = eventType;
        this.url = url;
        this.payload = payload;
    }

    public Webhook(Merchant merchant, Payment payment, String eventType, String url, Map<String, Object> payload) {
        this(merchant, eventType, url, payload);
        this.payment = payment;
    }

    // Business Methods
    public boolean isPending() {
        return WebhookStatus.PENDING.equals(this.status);
    }

    public boolean isSent() {
        return WebhookStatus.SENT.equals(this.status);
    }

    public boolean isDelivered() {
        return WebhookStatus.DELIVERED.equals(this.status);
    }

    public boolean isFailed() {
        return WebhookStatus.FAILED.equals(this.status);
    }

    public boolean canRetry() {
        return isFailed() && retryCount < maxRetries && !isDeleted();
    }

    public boolean isReadyForRetry() {
        return canRetry() && (nextRetryAt == null || Instant.now().isAfter(nextRetryAt));
    }

    public void markAsSent() {
        this.status = WebhookStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markAsDelivered(Integer responseStatus, String responseBody, Map<String, String> responseHeaders, Long responseTimeMs) {
        this.status = WebhookStatus.DELIVERED;
        this.deliveredAt = Instant.now();
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders != null ? responseHeaders : new HashMap<>();
        this.responseTimeMs = responseTimeMs;
    }

    public void markAsFailed(String errorMessage, String errorCode) {
        this.status = WebhookStatus.FAILED;
        this.failedAt = Instant.now();
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        scheduleRetry();
    }

    public void markAsFailed(Integer responseStatus, String responseBody, Map<String, String> responseHeaders, Long responseTimeMs, String errorMessage) {
        this.status = WebhookStatus.FAILED;
        this.failedAt = Instant.now();
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders != null ? responseHeaders : new HashMap<>();
        this.responseTimeMs = responseTimeMs;
        this.errorMessage = errorMessage;
        scheduleRetry();
    }

    public void scheduleRetry() {
        if (canRetry()) {
            this.retryCount++;
            // Exponential backoff: 60s, 120s, 240s, 480s, 960s
            long delaySeconds = retryDelaySeconds * (long) Math.pow(2, retryCount - 1);
            this.nextRetryAt = Instant.now().plusSeconds(delaySeconds);
        }
    }

    public void cancel() {
        this.status = WebhookStatus.CANCELLED;
    }

    public void addHeader(String name, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
    }

    public void updateMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    public boolean isSuccessful() {
        return responseStatus != null && responseStatus >= 200 && responseStatus < 300;
    }

    // Getters and Setters
    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public WebhookStatus getStatus() {
        return status;
    }

    public void setStatus(WebhookStatus status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Integer getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(Integer retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}