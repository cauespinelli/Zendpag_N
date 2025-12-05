package com.zendapag.core.events;

import com.zendapag.core.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a payment fails.
 */
public class PaymentFailedEvent extends DomainEvent {

    @NotNull
    private final String paymentId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final String referenceId;

    @NotNull
    private final BigDecimal amount;

    @NotNull
    private final String currency;

    @NotNull
    private final PaymentMethod paymentMethod;

    @NotNull
    private final String errorCode;

    @NotNull
    private final String errorMessage;

    private final Instant failedAt;
    private final String errorDetails;
    private final boolean retryable;

    public PaymentFailedEvent(String paymentId,
                             String merchantId,
                             String referenceId,
                             BigDecimal amount,
                             String currency,
                             PaymentMethod paymentMethod,
                             String errorCode,
                             String errorMessage,
                             String errorDetails,
                             boolean retryable,
                             String correlationId,
                             String causationId,
                             Long aggregateVersion) {

        super("payment_failed", paymentId, "Payment", correlationId, causationId, aggregateVersion);

        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.referenceId = referenceId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
        this.retryable = retryable;
        this.failedAt = Instant.now();

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("error_code", errorCode)
            .withMetadata("retryable", retryable);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", paymentId);
        data.put("merchantId", merchantId);
        data.put("referenceId", referenceId);
        data.put("amount", amount);
        data.put("currency", currency);
        data.put("paymentMethod", paymentMethod);
        data.put("errorCode", errorCode);
        data.put("errorMessage", errorMessage);
        data.put("errorDetails", errorDetails);
        data.put("retryable", retryable);
        data.put("failedAt", failedAt);
        return data;
    }

    public String getPaymentId() { return paymentId; }
    public String getMerchantId() { return merchantId; }
    public String getReferenceId() { return referenceId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public String getErrorDetails() { return errorDetails; }
    public boolean isRetryable() { return retryable; }
    public Instant getFailedAt() { return failedAt; }

    @Override
    public String getEventSummary() {
        return String.format("PaymentFailed[id=%s, error=%s, retryable=%s]", paymentId, errorCode, retryable);
    }
}
