package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a payment is refunded.
 */
public class PaymentRefundedEvent extends DomainEvent {

    @NotNull
    private final String paymentId;

    @NotNull
    private final String refundId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final BigDecimal originalAmount;

    @NotNull
    private final BigDecimal refundedAmount;

    private final String reason;
    private final boolean fullRefund;
    private final Instant refundedAt;
    private final String refundedBy;

    public PaymentRefundedEvent(String paymentId,
                               String refundId,
                               String merchantId,
                               BigDecimal originalAmount,
                               BigDecimal refundedAmount,
                               String reason,
                               String refundedBy,
                               String correlationId,
                               String causationId,
                               Long aggregateVersion) {

        super("payment_refunded", paymentId, "Payment", correlationId, causationId, aggregateVersion);

        this.paymentId = paymentId;
        this.refundId = refundId;
        this.merchantId = merchantId;
        this.originalAmount = originalAmount;
        this.refundedAmount = refundedAmount;
        this.reason = reason;
        this.fullRefund = originalAmount.compareTo(refundedAmount) == 0;
        this.refundedAt = Instant.now();
        this.refundedBy = refundedBy;

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("refund_id", refundId)
            .withMetadata("full_refund", fullRefund);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", paymentId);
        data.put("refundId", refundId);
        data.put("merchantId", merchantId);
        data.put("originalAmount", originalAmount);
        data.put("refundedAmount", refundedAmount);
        data.put("reason", reason);
        data.put("fullRefund", fullRefund);
        data.put("refundedAt", refundedAt);
        data.put("refundedBy", refundedBy);
        return data;
    }

    public String getPaymentId() { return paymentId; }
    public String getRefundId() { return refundId; }
    public String getMerchantId() { return merchantId; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public String getReason() { return reason; }
    public boolean isFullRefund() { return fullRefund; }
    public Instant getRefundedAt() { return refundedAt; }
    public String getRefundedBy() { return refundedBy; }

    @Override
    public String getEventSummary() {
        return String.format("PaymentRefunded[id=%s, refund=%s, amount=%s, full=%s]",
            paymentId, refundId, refundedAmount, fullRefund);
    }
}
