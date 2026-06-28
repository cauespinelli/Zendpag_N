package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.TransactionStatus;
import com.zendapag.core.entity.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_reference", columnList = "reference_id", unique = true),
    @Index(name = "idx_transaction_merchant", columnList = "merchant_id"),
    @Index(name = "idx_transaction_payment", columnList = "payment_id"),
    @Index(name = "idx_transaction_type", columnList = "type"),
    @Index(name = "idx_transaction_amount", columnList = "amount"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_transaction_settlement", columnList = "settlement_id"),
    @Index(name = "idx_transaction_created_at", columnList = "created_at"),
    @Index(name = "idx_transaction_deleted", columnList = "deleted"),
    @Index(name = "idx_transaction_composite", columnList = "merchant_id, type, transaction_date")
})
@SQLDelete(sql = "UPDATE transactions SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Transaction extends BaseEntity {

    @NotBlank(message = "Reference ID is required")
    @Size(min = 1, max = 100, message = "Reference ID must be between 1 and 100 characters")
    @Column(name = "reference_id", nullable = false, unique = true, length = 100)
    private String referenceId;

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_merchant"))
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", foreignKey = @ForeignKey(name = "fk_transaction_payment"))
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", foreignKey = @ForeignKey(name = "fk_transaction_settlement"))
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", foreignKey = @ForeignKey(name = "fk_transaction_account"))
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", foreignKey = @ForeignKey(name = "fk_transaction_source_account"))
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id", foreignKey = @ForeignKey(name = "fk_transaction_target_account"))
    private Account targetAccount;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @NotNull(message = "Transaction status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @NotNull(message = "Amount is required")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Transaction date is required")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Size(max = 500, message = "Error message must be at most 500 characters")
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "original_transaction_id")
    private UUID originalTransactionId;

    // Balance tracking
    @Column(name = "balance_before", precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    // Settlement tracking
    @Column(name = "settled", nullable = false)
    private Boolean settled = false;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    // Retenção pendente -> disponível (motor de saldo).
    // availableAt: quando o lançamento PAYMENT deixa de ser pendente e vira disponível.
    // released: marcado true pelo job de liberação após mover pendente -> disponível.
    @Column(name = "available_at")
    private Instant availableAt;

    @Column(name = "released", nullable = false)
    private Boolean released = false;

    // External references
    @Size(max = 255, message = "External reference must be at most 255 characters")
    @Column(name = "external_reference")
    private String externalReference;

    @Size(max = 255, message = "External transaction ID must be at most 255 characters")
    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    // Fee information
    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    // Metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata = new HashMap<>();

    // Additional transaction details
    @Size(max = 100, message = "Gateway must be at most 100 characters")
    @Column(name = "gateway", length = 100)
    private String gateway;

    @Size(max = 255, message = "Gateway transaction ID must be at most 255 characters")
    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    // PIX specific fields
    @Size(max = 100, message = "PIX transaction ID must be at most 100 characters")
    @Column(name = "pix_transaction_id", length = 100)
    private String pixTransactionId;

    @Size(max = 100, message = "PIX end to end ID must be at most 100 characters")
    @Column(name = "pix_end_to_end_id", length = 100)
    private String pixEndToEndId;

    // Reversal information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id", foreignKey = @ForeignKey(name = "fk_transaction_reversal_of"))
    private Transaction reversalOf;

    @Column(name = "reversed", nullable = false)
    private Boolean reversed = false;
    // Additional tracking fields for repository queries
    @Size(max = 255, message = "External ID must be at most 255 characters")
    @Column(name = "external_id")
    private String externalId;

    @Column(name = "parent_transaction_id")
    private UUID parentTransactionId;

    @Column(name = "reversal_transaction_id")
    private UUID reversalTransactionId;


    // Constructors
    public Transaction() {
        super();
        this.currency = "BRL";
        this.settled = false;
        this.reversed = false;
        this.status = TransactionStatus.PENDING;
        this.transactionDate = LocalDate.now();
    }

    public Transaction(String referenceId, Merchant merchant, TransactionType type, BigDecimal amount) {
        this();
        this.referenceId = referenceId;
        this.merchant = merchant;
        this.type = type;
        this.amount = amount;
        calculateNetAmount();
    }

    public Transaction(String referenceId, Merchant merchant, Payment payment, TransactionType type, BigDecimal amount) {
        this(referenceId, merchant, type, amount);
        this.payment = payment;
    }

    // Business Methods
    public boolean isCredit() {
        return TransactionType.PAYMENT.equals(type) ||
               TransactionType.REVERSAL.equals(type) ||
               TransactionType.ADJUSTMENT.equals(type) && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isDebit() {
        return TransactionType.REFUND.equals(type) ||
               TransactionType.CHARGEBACK.equals(type) ||
               TransactionType.FEE.equals(type) ||
               TransactionType.SETTLEMENT.equals(type) ||
               TransactionType.ADJUSTMENT.equals(type) && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public void settle(Settlement settlement) {
        if (this.settled) {
            throw new IllegalStateException("Transaction is already settled");
        }
        this.settlement = settlement;
        this.settled = true;
        this.settlementDate = settlement.getSettlementDate();
    }

    public void reverse(String reason) {
        if (this.reversed) {
            throw new IllegalStateException("Transaction is already reversed");
        }
        this.reversed = true;
        updateMetadata("reversal_reason", reason);
    }

    private void calculateNetAmount() {
        if (amount != null) {
            BigDecimal fees = feeAmount != null ? feeAmount : BigDecimal.ZERO;
            this.netAmount = amount.subtract(fees);
        }
    }

    public void updateBalance(BigDecimal previousBalance) {
        this.balanceBefore = previousBalance;
        if (isCredit()) {
            this.balanceAfter = previousBalance.add(amount);
        } else {
            this.balanceAfter = previousBalance.subtract(amount.abs());
        }
    }

    public void updateMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    public BigDecimal getSignedAmount() {
        if (isDebit()) {
            return amount.negate();
        }
        return amount;
    }

    // Getters and Setters
    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(Account sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public Account getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(Account targetAccount) {
        this.targetAccount = targetAccount;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Settlement getSettlement() {
        return settlement;
    }

    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        calculateNetAmount();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(UUID originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Boolean getSettled() {
        return settled;
    }

    public void setSettled(Boolean settled) {
        this.settled = settled;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(Instant availableAt) {
        this.availableAt = availableAt;
    }

    public Boolean getReleased() {
        return released;
    }

    public void setReleased(Boolean released) {
        this.released = released;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
        calculateNetAmount();
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getPixTransactionId() {
        return pixTransactionId;
    }

    public void setPixTransactionId(String pixTransactionId) {
        this.pixTransactionId = pixTransactionId;
    }

    public String getPixEndToEndId() {
        return pixEndToEndId;
    }

    public void setPixEndToEndId(String pixEndToEndId) {
        this.pixEndToEndId = pixEndToEndId;
    }

    public Transaction getReversalOf() {
        return reversalOf;
    }

    public void setReversalOf(Transaction reversalOf) {
        this.reversalOf = reversalOf;
    }

    public Boolean getReversed() {
        return reversed;
    }

    public void setReversed(Boolean reversed) {
        this.reversed = reversed;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public UUID getParentTransactionId() {
        return parentTransactionId;
    }

    public void setParentTransactionId(UUID parentTransactionId) {
        this.parentTransactionId = parentTransactionId;
    }

    public UUID getReversalTransactionId() {
        return reversalTransactionId;
    }

    public void setReversalTransactionId(UUID reversalTransactionId) {
        this.reversalTransactionId = reversalTransactionId;
    }

}