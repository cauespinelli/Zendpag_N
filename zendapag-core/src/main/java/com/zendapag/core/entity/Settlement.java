package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "settlements", indexes = {
    @Index(name = "idx_settlement_merchant", columnList = "merchant_id"),
    @Index(name = "idx_settlement_status", columnList = "status"),
    @Index(name = "idx_settlement_date", columnList = "settlement_date"),
    @Index(name = "idx_settlement_reference", columnList = "reference_id", unique = true),
    @Index(name = "idx_settlement_amount", columnList = "total_amount"),
    @Index(name = "idx_settlement_period", columnList = "period_start, period_end"),
    @Index(name = "idx_settlement_processed_at", columnList = "processed_at"),
    @Index(name = "idx_settlement_created_at", columnList = "created_at"),
    @Index(name = "idx_settlement_deleted", columnList = "deleted")
})
@SQLDelete(sql = "UPDATE settlements SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Settlement extends BaseEntity {

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_settlement_merchant"))
    private Merchant merchant;

    @NotBlank(message = "Reference ID is required")
    @Size(min = 1, max = 100, message = "Reference ID must be between 1 and 100 characters")
    @Column(name = "reference_id", nullable = false, unique = true, length = 100)
    private String referenceId;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    @NotNull(message = "Settlement date is required")
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @NotNull(message = "Period start is required")
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @NotNull(message = "Period end is required")
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    // Financial Information
    @NotNull(message = "Total amount is required")
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "gross_amount", precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "chargeback_amount", precision = 15, scale = 2)
    private BigDecimal chargebackAmount = BigDecimal.ZERO;

    @Column(name = "adjustment_amount", precision = 15, scale = 2)
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    // Transaction Counts
    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount = 0;

    @Column(name = "payment_count", nullable = false)
    private Integer paymentCount = 0;

    @Column(name = "refund_count", nullable = false)
    private Integer refundCount = 0;

    @Column(name = "chargeback_count", nullable = false)
    private Integer chargebackCount = 0;

    // Banking Information
    @Size(max = 10, message = "Bank code must be at most 10 characters")
    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Size(max = 100, message = "Bank name must be at most 100 characters")
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Size(max = 20, message = "Agency must be at most 20 characters")
    @Column(name = "agency", length = 20)
    private String agency;

    @Size(max = 20, message = "Account must be at most 20 characters")
    @Column(name = "account", length = 20)
    private String account;

    @Size(max = 10, message = "Account type must be at most 10 characters")
    @Column(name = "account_type", length = 10)
    private String accountType;

    @Size(max = 100, message = "Account holder name must be at most 100 characters")
    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Size(max = 14, message = "Account holder document must be at most 14 characters")
    @Column(name = "account_holder_document", length = 14)
    private String accountHolderDocument;

    // Processing Information
    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    // External References
    @Size(max = 255, message = "External ID must be at most 255 characters")
    @Column(name = "external_id")
    private String externalId;

    @Size(max = 255, message = "Bank transaction ID must be at most 255 characters")
    @Column(name = "bank_transaction_id")
    private String bankTransactionId;

    @Size(max = 100, message = "Gateway must be at most 100 characters")
    @Column(name = "gateway", length = 100)
    private String gateway;

    // Failure Information
    @Size(max = 1000, message = "Failure reason must be at most 1000 characters")
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Size(max = 50, message = "Failure code must be at most 50 characters")
    @Column(name = "failure_code", length = 50)
    private String failureCode;

    // Additional Information
    @Size(max = 500, message = "Description must be at most 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    // Relationships
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Transaction> transactions = new HashSet<>();

    // Constructors
    public Settlement() {
        super();
        this.status = SettlementStatus.PENDING;
        this.currency = "BRL";
        this.chargebackAmount = BigDecimal.ZERO;
        this.adjustmentAmount = BigDecimal.ZERO;
        this.transactionCount = 0;
        this.paymentCount = 0;
        this.refundCount = 0;
        this.chargebackCount = 0;
    }

    public Settlement(Merchant merchant, String referenceId, LocalDate settlementDate, LocalDate periodStart, LocalDate periodEnd) {
        this();
        this.merchant = merchant;
        this.referenceId = referenceId;
        this.settlementDate = settlementDate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Business Methods
    public boolean isPending() {
        return SettlementStatus.PENDING.equals(this.status);
    }

    public boolean isProcessing() {
        return SettlementStatus.PROCESSING.equals(this.status);
    }

    public boolean isSettled() {
        return SettlementStatus.SETTLED.equals(this.status);
    }

    public boolean isFailed() {
        return SettlementStatus.FAILED.equals(this.status);
    }

    public boolean isCancelled() {
        return SettlementStatus.CANCELLED.equals(this.status);
    }

    public void process() {
        if (!isPending()) {
            throw new IllegalStateException("Settlement can only be processed from PENDING status");
        }
        this.status = SettlementStatus.PROCESSING;
        this.processedAt = Instant.now();
    }

    public void settle() {
        if (!isProcessing()) {
            throw new IllegalStateException("Settlement can only be settled from PROCESSING status");
        }
        this.status = SettlementStatus.SETTLED;
        this.settledAt = Instant.now();
    }

    public void fail(String reason, String code) {
        if (!isPending() && !isProcessing()) {
            throw new IllegalStateException("Settlement can only be failed from PENDING or PROCESSING status");
        }
        this.status = SettlementStatus.FAILED;
        this.failedAt = Instant.now();
        this.failureReason = reason;
        this.failureCode = code;
    }

    public void cancel() {
        if (isSettled()) {
            throw new IllegalStateException("Cannot cancel a settled settlement");
        }
        this.status = SettlementStatus.CANCELLED;
    }

    public void calculateAmounts() {
        if (grossAmount != null && feeAmount != null) {
            this.totalAmount = grossAmount.subtract(feeAmount).subtract(chargebackAmount).add(adjustmentAmount);
            this.netAmount = totalAmount;
        }
    }

    public void addTransaction(Transaction transaction) {
        if (transactions == null) {
            transactions = new HashSet<>();
        }
        transactions.add(transaction);
        transaction.setSettlement(this);

        // Update counters and amounts
        this.transactionCount++;

        switch (transaction.getType()) {
            case PAYMENT:
                this.paymentCount++;
                break;
            case REFUND:
                this.refundCount++;
                break;
            case CHARGEBACK:
                this.chargebackCount++;
                break;
        }
    }

    public void updateMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    public boolean isInPeriod(LocalDate date) {
        return !date.isBefore(periodStart) && !date.isAfter(periodEnd);
    }

    public long getPeriodDays() {
        return periodStart.until(periodEnd).getDays() + 1;
    }

    // Getters and Setters
    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
        calculateAmounts();
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
        calculateAmounts();
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getChargebackAmount() {
        return chargebackAmount;
    }

    public void setChargebackAmount(BigDecimal chargebackAmount) {
        this.chargebackAmount = chargebackAmount;
        calculateAmounts();
    }

    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public void setAdjustmentAmount(BigDecimal adjustmentAmount) {
        this.adjustmentAmount = adjustmentAmount;
        calculateAmounts();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Integer getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(Integer paymentCount) {
        this.paymentCount = paymentCount;
    }

    public Integer getRefundCount() {
        return refundCount;
    }

    public void setRefundCount(Integer refundCount) {
        this.refundCount = refundCount;
    }

    public Integer getChargebackCount() {
        return chargebackCount;
    }

    public void setChargebackCount(Integer chargebackCount) {
        this.chargebackCount = chargebackCount;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountHolderDocument() {
        return accountHolderDocument;
    }

    public void setAccountHolderDocument(String accountHolderDocument) {
        this.accountHolderDocument = accountHolderDocument;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(Instant settledAt) {
        this.settledAt = settledAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getBankTransactionId() {
        return bankTransactionId;
    }

    public void setBankTransactionId(String bankTransactionId) {
        this.bankTransactionId = bankTransactionId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Set<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(Set<Transaction> transactions) {
        this.transactions = transactions;
    }

    // Alias for settledAt - used by ReportService
    public Instant getCompletedAt() {
        return settledAt;
    }
}
