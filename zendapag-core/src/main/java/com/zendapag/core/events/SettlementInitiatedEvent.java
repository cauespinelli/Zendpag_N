package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event published when a settlement is initiated.
 */
public class SettlementInitiatedEvent extends DomainEvent {

    @NotNull
    private final String settlementId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final BigDecimal totalAmount;

    @NotNull
    private final BigDecimal netAmount;

    private final BigDecimal totalFees;
    private final int transactionCount;
    private final List<String> transactionIds;
    private final Instant initiatedAt;
    private final String settlementType;

    public SettlementInitiatedEvent(String settlementId,
                                   String merchantId,
                                   BigDecimal totalAmount,
                                   BigDecimal netAmount,
                                   BigDecimal totalFees,
                                   int transactionCount,
                                   List<String> transactionIds,
                                   String settlementType,
                                   String correlationId) {

        super("settlement_initiated", settlementId, "Settlement", correlationId, null, 1L);

        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.totalAmount = totalAmount;
        this.netAmount = netAmount;
        this.totalFees = totalFees;
        this.transactionCount = transactionCount;
        this.transactionIds = transactionIds;
        this.initiatedAt = Instant.now();
        this.settlementType = settlementType;

        this.withMetadata("merchant_id", merchantId)
            .withMetadata("total_amount", totalAmount.toString())
            .withMetadata("transaction_count", transactionCount);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", settlementId);
        data.put("merchantId", merchantId);
        data.put("totalAmount", totalAmount);
        data.put("netAmount", netAmount);
        data.put("totalFees", totalFees);
        data.put("transactionCount", transactionCount);
        data.put("transactionIds", transactionIds);
        data.put("initiatedAt", initiatedAt);
        data.put("settlementType", settlementType);
        return data;
    }

    public String getSettlementId() { return settlementId; }
    public String getMerchantId() { return merchantId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public BigDecimal getTotalFees() { return totalFees; }
    public int getTransactionCount() { return transactionCount; }
    public List<String> getTransactionIds() { return transactionIds; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public String getSettlementType() { return settlementType; }

    @Override
    public String getEventSummary() {
        return String.format("SettlementInitiated[id=%s, merchant=%s, amount=%s, txns=%d]",
            settlementId, merchantId, totalAmount, transactionCount);
    }
}
