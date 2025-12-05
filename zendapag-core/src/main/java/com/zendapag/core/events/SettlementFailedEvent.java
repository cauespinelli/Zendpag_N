package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a settlement fails.
 */
public class SettlementFailedEvent extends DomainEvent {

    @NotNull
    private final String settlementId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final BigDecimal amount;

    @NotNull
    private final String errorCode;

    @NotNull
    private final String errorMessage;

    private final String errorDetails;
    private final boolean retryable;
    private final int attemptNumber;
    private final Instant failedAt;

    public SettlementFailedEvent(String settlementId,
                                String merchantId,
                                BigDecimal amount,
                                String errorCode,
                                String errorMessage,
                                String errorDetails,
                                boolean retryable,
                                int attemptNumber,
                                String correlationId,
                                String causationId,
                                Long aggregateVersion) {

        super("settlement_failed", settlementId, "Settlement", correlationId, causationId, aggregateVersion);

        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
        this.retryable = retryable;
        this.attemptNumber = attemptNumber;
        this.failedAt = Instant.now();

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("error_code", errorCode)
            .withMetadata("retryable", retryable);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", settlementId);
        data.put("merchantId", merchantId);
        data.put("amount", amount);
        data.put("errorCode", errorCode);
        data.put("errorMessage", errorMessage);
        data.put("errorDetails", errorDetails);
        data.put("retryable", retryable);
        data.put("attemptNumber", attemptNumber);
        data.put("failedAt", failedAt);
        return data;
    }

    public String getSettlementId() { return settlementId; }
    public String getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public String getErrorDetails() { return errorDetails; }
    public boolean isRetryable() { return retryable; }
    public int getAttemptNumber() { return attemptNumber; }
    public Instant getFailedAt() { return failedAt; }

    @Override
    public String getEventSummary() {
        return String.format("SettlementFailed[id=%s, error=%s, retryable=%s]", settlementId, errorCode, retryable);
    }
}
