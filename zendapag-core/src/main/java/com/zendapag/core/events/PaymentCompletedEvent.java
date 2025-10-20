package com.zendapag.core.events;

import com.zendapag.core.enums.PaymentMethod;
import com.zendapag.core.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a payment is successfully completed.
 * This triggers settlement, notifications, and webhook delivery processes.
 */
public class PaymentCompletedEvent extends DomainEvent {

    @NotNull
    private final String paymentId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final String referenceId;

    @NotNull
    @Positive
    private final BigDecimal amount;

    @NotNull
    private final String currency;

    @NotNull
    private final PaymentMethod paymentMethod;

    @NotNull
    private final Instant completedAt;

    private final String pixTxId;
    private final String externalTransactionId;
    private final BigDecimal netAmount;
    private final BigDecimal feeAmount;
    private final String customerName;
    private final String customerDocument;
    private final Map<String, Object> paymentDetails;

    public PaymentCompletedEvent(String paymentId,
                               String merchantId,
                               String referenceId,
                               BigDecimal amount,
                               String currency,
                               PaymentMethod paymentMethod,
                               Instant completedAt,
                               String pixTxId,
                               String externalTransactionId,
                               BigDecimal netAmount,
                               BigDecimal feeAmount,
                               String customerName,
                               String customerDocument,
                               Map<String, Object> paymentDetails,
                               String correlationId,
                               String causationId,
                               Long aggregateVersion) {

        super("payment_completed", paymentId, "Payment", correlationId, causationId, aggregateVersion);

        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.referenceId = referenceId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.completedAt = completedAt;
        this.pixTxId = pixTxId;
        this.externalTransactionId = externalTransactionId;
        this.netAmount = netAmount;
        this.feeAmount = feeAmount;
        this.customerName = customerName;
        this.customerDocument = customerDocument;
        this.paymentDetails = paymentDetails != null ? new HashMap<>(paymentDetails) : new HashMap<>();

        // Add contextual metadata
        this.withMetadata("merchant_id", merchantId)
            .withMetadata("payment_method", paymentMethod.toString())
            .withMetadata("amount", amount.toString())
            .withMetadata("net_amount", netAmount != null ? netAmount.toString() : null)
            .withMetadata("fee_amount", feeAmount != null ? feeAmount.toString() : null)
            .withMetadata("currency", currency)
            .withMetadata("completed_at", completedAt.toString())
            .withMetadata("requires_settlement", shouldTriggerSettlement())
            .withMetadata("requires_webhook", true)
            .withMetadata("requires_notification", true);
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
        data.put("completedAt", completedAt);
        data.put("pixTxId", pixTxId);
        data.put("externalTransactionId", externalTransactionId);
        data.put("netAmount", netAmount);
        data.put("feeAmount", feeAmount);
        data.put("customerName", customerName);
        data.put("customerDocument", customerDocument);
        data.put("paymentDetails", paymentDetails);
        return data;
    }

    // Getters
    public String getPaymentId() {
        return paymentId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getPixTxId() {
        return pixTxId;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public Map<String, Object> getPaymentDetails() {
        return new HashMap<>(paymentDetails);
    }

    // Business logic methods
    public boolean shouldTriggerSettlement() {
        // Settlement is required if we have net amount and it's above minimum threshold
        return netAmount != null && netAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean shouldTriggerWebhook() {
        return true; // All completed payments trigger webhooks
    }

    public boolean shouldTriggerNotification() {
        return true; // All completed payments trigger notifications
    }

    public boolean isPixPayment() {
        return PaymentMethod.PIX.equals(paymentMethod);
    }

    public boolean hasPixTransaction() {
        return pixTxId != null && !pixTxId.isEmpty();
    }

    public boolean hasExternalTransaction() {
        return externalTransactionId != null && !externalTransactionId.isEmpty();
    }

    public boolean hasFees() {
        return feeAmount != null && feeAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getEffectiveFeeAmount() {
        return feeAmount != null ? feeAmount : BigDecimal.ZERO;
    }

    public BigDecimal getEffectiveNetAmount() {
        if (netAmount != null) {
            return netAmount;
        }
        // Calculate net amount if not provided
        return amount.subtract(getEffectiveFeeAmount());
    }

    public long getProcessingTimeMillis() {
        // Assumes the payment was created at the same time as this event minus some processing time
        // In practice, you'd want to pass the original creation time
        return getAgeInMillis();
    }

    public String getPaymentDetailValue(String key) {
        Object value = paymentDetails.get(key);
        return value != null ? value.toString() : null;
    }

    // Validation methods
    public boolean hasRequiredFields() {
        return paymentId != null &&
               merchantId != null &&
               amount != null &&
               currency != null &&
               paymentMethod != null &&
               completedAt != null;
    }

    public boolean isValidAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isValidNetAmount() {
        return netAmount == null || netAmount.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean isValidFeeAmount() {
        return feeAmount == null ||
               (feeAmount.compareTo(BigDecimal.ZERO) >= 0 &&
                feeAmount.compareTo(amount) <= 0);
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
               hasRequiredFields() &&
               isValidAmount() &&
               isValidNetAmount() &&
               isValidFeeAmount();
    }

    @Override
    public String getEventSummary() {
        return String.format("PaymentCompleted[id=%s, merchant=%s, amount=%s %s, net=%s, method=%s]",
                paymentId, merchantId, amount, currency,
                netAmount != null ? netAmount : "calculated", paymentMethod);
    }
}