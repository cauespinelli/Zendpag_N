package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a payment is cancelled.
 */
public class PaymentCancelledEvent extends DomainEvent {

    @NotNull
    private final String paymentId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final BigDecimal amount;

    @NotNull
    private final String reason;

    private final String cancelledBy;
    private final Instant cancelledAt;

    public PaymentCancelledEvent(String paymentId,
                                String merchantId,
                                BigDecimal amount,
                                String reason,
                                String cancelledBy,
                                String correlationId,
                                String causationId,
                                Long aggregateVersion) {

        super("payment_cancelled", paymentId, "Payment", correlationId, causationId, aggregateVersion);

        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.reason = reason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = Instant.now();

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("cancelled_by", cancelledBy)
            .withMetadata("reason", reason);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", paymentId);
        data.put("merchantId", merchantId);
        data.put("amount", amount);
        data.put("reason", reason);
        data.put("cancelledBy", cancelledBy);
        data.put("cancelledAt", cancelledAt);
        return data;
    }

    public String getPaymentId() { return paymentId; }
    public String getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public String getCancelledBy() { return cancelledBy; }
    public Instant getCancelledAt() { return cancelledAt; }

    @Override
    public String getEventSummary() {
        return String.format("PaymentCancelled[id=%s, reason=%s]", paymentId, reason);
    }
}
