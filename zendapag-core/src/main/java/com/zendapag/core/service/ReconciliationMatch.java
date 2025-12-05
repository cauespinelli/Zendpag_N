package com.zendapag.core.service;

import java.math.BigDecimal;
import java.time.Instant;

public class ReconciliationMatch {

    private String paymentId;
    private String externalId;
    private BigDecimal amount;
    private String status;
    private Instant matchedAt;
    private String matchType;
    private String description;

    public ReconciliationMatch() {
    }

    public ReconciliationMatch(String paymentId, String externalId, BigDecimal amount, String status) {
        this.paymentId = paymentId;
        this.externalId = externalId;
        this.amount = amount;
        this.status = status;
        this.matchedAt = Instant.now();
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(Instant matchedAt) {
        this.matchedAt = matchedAt;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ReconciliationMatch{" +
                "paymentId='" + paymentId + '\'' +
                ", externalId='" + externalId + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", matchedAt=" + matchedAt +
                '}';
    }
}
