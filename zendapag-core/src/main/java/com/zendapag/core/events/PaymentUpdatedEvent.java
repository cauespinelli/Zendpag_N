package com.zendapag.core.events;

import com.zendapag.core.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a payment is updated.
 */
public class PaymentUpdatedEvent extends DomainEvent {

    @NotNull
    private final String paymentId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final PaymentStatus previousStatus;

    @NotNull
    private final PaymentStatus newStatus;

    private final String reason;
    private final Instant updatedAt;
    private final Map<String, Object> changes;

    public PaymentUpdatedEvent(String paymentId,
                              String merchantId,
                              PaymentStatus previousStatus,
                              PaymentStatus newStatus,
                              String reason,
                              Map<String, Object> changes,
                              String correlationId,
                              String causationId,
                              Long aggregateVersion) {

        super("payment_updated", paymentId, "Payment", correlationId, causationId, aggregateVersion);

        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.updatedAt = Instant.now();
        this.changes = changes != null ? new HashMap<>(changes) : new HashMap<>();

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("previous_status", previousStatus.toString())
            .withMetadata("new_status", newStatus.toString());
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", paymentId);
        data.put("merchantId", merchantId);
        data.put("previousStatus", previousStatus);
        data.put("newStatus", newStatus);
        data.put("reason", reason);
        data.put("updatedAt", updatedAt);
        data.put("changes", changes);
        return data;
    }

    public String getPaymentId() { return paymentId; }
    public String getMerchantId() { return merchantId; }
    public PaymentStatus getPreviousStatus() { return previousStatus; }
    public PaymentStatus getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Map<String, Object> getChanges() { return new HashMap<>(changes); }

    @Override
    public String getEventSummary() {
        return String.format("PaymentUpdated[id=%s, %s -> %s]", paymentId, previousStatus, newStatus);
    }
}
