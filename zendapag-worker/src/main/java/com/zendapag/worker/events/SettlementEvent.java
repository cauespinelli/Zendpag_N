package com.zendapag.worker.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class SettlementEvent {

    private UUID settlementId;
    private UUID merchantId;
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;
    private SettlementType type;
    private String period;
    private LocalDate settlementDate;

    public enum SettlementType {
        CREATED, PROCESSED, COMPLETED, FAILED, CANCELLED
    }

    public SettlementEvent() {}

    public SettlementEvent(UUID settlementId, UUID merchantId, BigDecimal amount, SettlementType type) {
        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = "BRL";
        this.createdAt = Instant.now();
        this.type = type;
    }

    public UUID getSettlementId() { return settlementId; }
    public void setSettlementId(UUID settlementId) { this.settlementId = settlementId; }
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public SettlementType getType() { return type; }
    public void setType(SettlementType type) { this.type = type; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
}
