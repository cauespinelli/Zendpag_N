package com.zendapag.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO representing an external PIX transaction from the participant system
 * Used for reconciliation against internal transactions
 */
public class PixTransaction {

    @JsonProperty("endToEndId")
    private String endToEndId;

    @JsonProperty("txid")
    private String txid;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("settledAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime settledAt;

    @JsonProperty("payerDocument")
    private String payerDocument;

    @JsonProperty("payerName")
    private String payerName;

    @JsonProperty("payeeDocument")
    private String payeeDocument;

    @JsonProperty("payeeName")
    private String payeeName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("institutionCode")
    private String institutionCode;

    @JsonProperty("branchCode")
    private String branchCode;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("transactionType")
    private String transactionType;

    @JsonProperty("originalAmount")
    private BigDecimal originalAmount;

    @JsonProperty("fees")
    private BigDecimal fees;

    @JsonProperty("netAmount")
    private BigDecimal netAmount;

    public PixTransaction() {}

    public PixTransaction(String endToEndId, String txid, BigDecimal amount, String status) {
        this.endToEndId = endToEndId;
        this.txid = txid;
        this.amount = amount;
        this.status = status;
    }

    /**
     * Get unique transaction identifier for reconciliation
     * Uses endToEndId as primary key, falls back to txid
     */
    public String getId() {
        return endToEndId != null ? endToEndId : txid;
    }

    /**
     * Check if transaction is settled/completed
     */
    public boolean isSettled() {
        return "SETTLED".equalsIgnoreCase(status) ||
               "COMPLETED".equalsIgnoreCase(status) ||
               "CONFIRMED".equalsIgnoreCase(status);
    }

    /**
     * Check if transaction is failed
     */
    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status) ||
               "REJECTED".equalsIgnoreCase(status) ||
               "CANCELLED".equalsIgnoreCase(status);
    }

    /**
     * Check if transaction is pending
     */
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status) ||
               "PROCESSING".equalsIgnoreCase(status);
    }

    /**
     * Get the effective amount for reconciliation
     * Uses netAmount if available, otherwise original amount
     */
    public BigDecimal getEffectiveAmount() {
        return netAmount != null ? netAmount :
               (originalAmount != null ? originalAmount : amount);
    }

    /**
     * Convert to normalized transaction status
     */
    public String getNormalizedStatus() {
        if (isSettled()) return "COMPLETED";
        if (isFailed()) return "FAILED";
        if (isPending()) return "PENDING";
        return status != null ? status.toUpperCase() : "UNKNOWN";
    }

    /**
     * Create a summary string for logging
     */
    public String getSummary() {
        return String.format("PIX[%s]: %s %s - %s",
            getId(),
            getEffectiveAmount(),
            getNormalizedStatus(),
            description != null ? description.substring(0, Math.min(50, description.length())) : "");
    }

    // Getters and setters
    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }

    public String getTxid() { return txid; }
    public void setTxid(String txid) { this.txid = txid; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }

    public String getPayerDocument() { return payerDocument; }
    public void setPayerDocument(String payerDocument) { this.payerDocument = payerDocument; }

    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }

    public String getPayeeDocument() { return payeeDocument; }
    public void setPayeeDocument(String payeeDocument) { this.payeeDocument = payeeDocument; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInstitutionCode() { return institutionCode; }
    public void setInstitutionCode(String institutionCode) { this.institutionCode = institutionCode; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PixTransaction that = (PixTransaction) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "PixTransaction{" +
                "endToEndId='" + endToEndId + '\'' +
                ", txid='" + txid + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}