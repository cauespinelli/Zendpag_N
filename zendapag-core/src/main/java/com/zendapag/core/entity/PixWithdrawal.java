package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.WithdrawalStatus;
import com.zendapag.core.entity.enums.WithdrawalTriggerType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entidade que representa um saque PIX
 */
@Entity
@Table(name = "pix_withdrawals", indexes = {
    @Index(name = "idx_withdrawal_reference", columnList = "reference_id", unique = true),
    @Index(name = "idx_withdrawal_account", columnList = "account_id"),
    @Index(name = "idx_withdrawal_merchant", columnList = "merchant_id"),
    @Index(name = "idx_withdrawal_status", columnList = "status"),
    @Index(name = "idx_withdrawal_created_at", columnList = "created_at"),
    @Index(name = "idx_withdrawal_pix_key", columnList = "pix_key"),
    @Index(name = "idx_withdrawal_deleted", columnList = "deleted"),
    @Index(name = "idx_withdrawal_composite", columnList = "merchant_id, status, created_at")
})
@SQLDelete(sql = "UPDATE pix_withdrawals SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class PixWithdrawal extends BaseEntity {

    @NotBlank(message = "Reference ID is required")
    @Size(min = 1, max = 100, message = "Reference ID must be between 1 and 100 characters")
    @Column(name = "reference_id", nullable = false, unique = true, length = 100)
    private String referenceId;

    @NotNull(message = "Account is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_withdrawal_account"))
    private Account account;

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_withdrawal_merchant"))
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", foreignKey = @ForeignKey(name = "fk_withdrawal_transaction"))
    private Transaction transaction;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    @NotBlank(message = "PIX key is required")
    @Size(max = 255, message = "PIX key must be at most 255 characters")
    @Column(name = "pix_key", nullable = false)
    private String pixKey;

    @NotBlank(message = "PIX key type is required")
    @Size(max = 20, message = "PIX key type must be at most 20 characters")
    @Column(name = "pix_key_type", nullable = false, length = 20)
    private String pixKeyType; // CPF, CNPJ, EMAIL, PHONE, RANDOM

    @Size(max = 255, message = "Recipient name must be at most 255 characters")
    @Column(name = "recipient_name")
    private String recipientName;

    @Size(max = 20, message = "Recipient document must be at most 20 characters")
    @Column(name = "recipient_document", length = 20)
    private String recipientDocument;

    @Size(max = 100, message = "Recipient bank must be at most 100 characters")
    @Column(name = "recipient_bank", length = 100)
    private String recipientBank;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 1000, message = "Rejection reason must be at most 1000 characters")
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    // Origem do saque: MANUAL (solicitado) ou AUTOMATIC (auto-payout do motor).
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private WithdrawalTriggerType triggerType = WithdrawalTriggerType.MANUAL;

    // PIX specific fields
    @Size(max = 100, message = "PIX transaction ID must be at most 100 characters")
    @Column(name = "pix_transaction_id", length = 100)
    private String pixTransactionId;

    @Size(max = 100, message = "PIX end to end ID must be at most 100 characters")
    @Column(name = "pix_end_to_end_id", length = 100)
    private String pixEndToEndId;

    @Size(max = 100, message = "PIX return ID must be at most 100 characters")
    @Column(name = "pix_return_id", length = 100)
    private String pixReturnId;

    // External references
    @Size(max = 255, message = "External reference must be at most 255 characters")
    @Column(name = "external_reference")
    private String externalReference;

    @Size(max = 255, message = "External transaction ID must be at most 255 characters")
    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    // Timestamps
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Balance tracking
    @Column(name = "balance_before", precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    // Metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata = new HashMap<>();

    // Security fields
    @Size(max = 50, message = "IP address must be at most 50 characters")
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Size(max = 500, message = "User agent must be at most 500 characters")
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // Constructors
    public PixWithdrawal() {
        super();
        this.status = WithdrawalStatus.PENDING;
        this.requestedAt = Instant.now();
        this.feeAmount = BigDecimal.ZERO;
    }

    public PixWithdrawal(String referenceId, Account account, Merchant merchant,
                        BigDecimal amount, String pixKey, String pixKeyType) {
        this();
        this.referenceId = referenceId;
        this.account = account;
        this.merchant = merchant;
        this.amount = amount;
        this.pixKey = pixKey;
        this.pixKeyType = pixKeyType;
        calculateNetAmount();
    }

    // Business Methods
    public void approve() {
        if (this.status != WithdrawalStatus.PENDING && this.status != WithdrawalStatus.PROCESSING) {
            throw new IllegalStateException("Only PENDING or PROCESSING withdrawals can be approved");
        }
        this.status = WithdrawalStatus.APPROVED;
        this.processedAt = Instant.now();
    }

    public void complete(String pixEndToEndId) {
        if (this.status != WithdrawalStatus.APPROVED && this.status != WithdrawalStatus.PROCESSING) {
            throw new IllegalStateException("Only APPROVED or PROCESSING withdrawals can be completed");
        }
        this.status = WithdrawalStatus.COMPLETED;
        this.pixEndToEndId = pixEndToEndId;
        this.completedAt = Instant.now();
    }

    public void reject(String reason) {
        if (this.status == WithdrawalStatus.COMPLETED || this.status == WithdrawalStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reject a " + this.status + " withdrawal");
        }
        this.status = WithdrawalStatus.REJECTED;
        this.rejectionReason = reason;
        this.processedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == WithdrawalStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed withdrawal");
        }
        this.status = WithdrawalStatus.CANCELLED;
        this.rejectionReason = reason;
        this.cancelledAt = Instant.now();
    }

    public void markAsFailed(String reason) {
        this.status = WithdrawalStatus.FAILED;
        this.rejectionReason = reason;
        this.processedAt = Instant.now();
    }

    public void startProcessing() {
        if (this.status != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING withdrawals can start processing");
        }
        this.status = WithdrawalStatus.PROCESSING;
    }

    public void reverse(String reason) {
        if (this.status != WithdrawalStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED withdrawals can be reversed");
        }
        this.status = WithdrawalStatus.REVERSED;
        this.rejectionReason = reason;
        updateMetadata("reversal_timestamp", Instant.now().toString());
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
        this.balanceAfter = previousBalance.subtract(amount);
    }

    public void updateMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean canBeCancelled() {
        return status == WithdrawalStatus.PENDING || status == WithdrawalStatus.PROCESSING;
    }

    public boolean isCompleted() {
        return status == WithdrawalStatus.COMPLETED;
    }

    public boolean isFinal() {
        return status == WithdrawalStatus.COMPLETED ||
               status == WithdrawalStatus.REJECTED ||
               status == WithdrawalStatus.CANCELLED ||
               status == WithdrawalStatus.REVERSED;
    }

    // Getters and Setters
    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        calculateNetAmount();
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

    public String getPixKey() {
        return pixKey;
    }

    public void setPixKey(String pixKey) {
        this.pixKey = pixKey;
    }

    public String getPixKeyType() {
        return pixKeyType;
    }

    public void setPixKeyType(String pixKeyType) {
        this.pixKeyType = pixKeyType;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientDocument() {
        return recipientDocument;
    }

    public void setRecipientDocument(String recipientDocument) {
        this.recipientDocument = recipientDocument;
    }

    public String getRecipientBank() {
        return recipientBank;
    }

    public void setRecipientBank(String recipientBank) {
        this.recipientBank = recipientBank;
    }

    public WithdrawalStatus getStatus() {
        return status;
    }

    public void setStatus(WithdrawalStatus status) {
        this.status = status;
    }

    public WithdrawalTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(WithdrawalTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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

    public String getPixReturnId() {
        return pixReturnId;
    }

    public void setPixReturnId(String pixReturnId) {
        this.pixReturnId = pixReturnId;
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

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
