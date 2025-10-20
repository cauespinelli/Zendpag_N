package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_reference", columnList = "reference_id", unique = true),
    @Index(name = "idx_payment_merchant", columnList = "merchant_id"),
    @Index(name = "idx_payment_customer", columnList = "customer_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_amount", columnList = "amount"),
    @Index(name = "idx_payment_created_at", columnList = "created_at"),
    @Index(name = "idx_payment_expires_at", columnList = "expires_at"),
    @Index(name = "idx_payment_processed_at", columnList = "processed_at"),
    @Index(name = "idx_payment_deleted", columnList = "deleted")
})
@SQLDelete(sql = "UPDATE payments SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Payment extends BaseEntity {

    @NotBlank(message = "Reference ID is required")
    @Size(min = 1, max = 100, message = "Reference ID must be between 1 and 100 characters")
    @Column(name = "reference_id", nullable = false, unique = true, length = 100)
    private String referenceId;

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_merchant"))
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "fk_payment_customer"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", foreignKey = @ForeignKey(name = "fk_payment_method"))
    private PaymentMethod paymentMethod;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @DecimalMax(value = "999999999.99", message = "Amount cannot exceed 999,999,999.99")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private PaymentStatus status;

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    // Fee Information
    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "fee_rate", precision = 5, scale = 4)
    private BigDecimal feeRate;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    // Payment Flow
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    // External References
    @Size(max = 255, message = "External ID must be at most 255 characters")
    @Column(name = "external_id")
    private String externalId;

    @Size(max = 255, message = "Gateway transaction ID must be at most 255 characters")
    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Size(max = 100, message = "Gateway must be at most 100 characters")
    @Column(name = "gateway", length = 100)
    private String gateway;

    // PIX specific fields
    @Size(max = 255, message = "PIX key must be at most 255 characters")
    @Column(name = "pix_key")
    private String pixKey;

    @Size(max = 500, message = "PIX QR code must be at most 500 characters")
    @Column(name = "pix_qr_code", length = 500)
    private String pixQrCode;

    @Size(max = 100, message = "PIX transaction ID must be at most 100 characters")
    @Column(name = "pix_transaction_id", length = 100)
    private String pixTransactionId;

    // Failure Information
    @Size(max = 255, message = "Failure reason must be at most 255 characters")
    @Column(name = "failure_reason")
    private String failureReason;

    @Size(max = 50, message = "Failure code must be at most 50 characters")
    @Column(name = "failure_code", length = 50)
    private String failureCode;

    // Refund Information
    @Column(name = "refunded_amount", precision = 15, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "refundable_amount", precision = 15, scale = 2)
    private BigDecimal refundableAmount;

    // Additional Data
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // Customer Information (for anonymous payments)
    @Size(max = 255, message = "Customer name must be at most 255 characters")
    @Column(name = "customer_name")
    private String customerName;

    @Email(message = "Customer email must be valid")
    @Size(max = 255, message = "Customer email must be at most 255 characters")
    @Column(name = "customer_email")
    private String customerEmail;

    @Size(max = 20, message = "Customer phone must be at most 20 characters")
    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Size(max = 14, message = "Customer document must be at most 14 characters")
    @Column(name = "customer_document", length = 14)
    private String customerDocument;

    // URLs
    @Size(max = 500, message = "Return URL must be at most 500 characters")
    @Column(name = "return_url", length = 500)
    private String returnUrl;

    @Size(max = 500, message = "Cancel URL must be at most 500 characters")
    @Column(name = "cancel_url", length = 500)
    private String cancelUrl;

    @Size(max = 500, message = "Notification URL must be at most 500 characters")
    @Column(name = "notification_url", length = 500)
    private String notificationUrl;

    // Request Information
    @Size(max = 45, message = "IP address must be at most 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 500, message = "User agent must be at most 500 characters")
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // Relationships
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Transaction> transactions = new HashSet<>();

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Webhook> webhooks = new HashSet<>();

    // Constructors
    public Payment() {
        super();
        this.status = PaymentStatus.PENDING;
        this.currency = "BRL";
        this.refundedAmount = BigDecimal.ZERO;
    }

    public Payment(String referenceId, Merchant merchant, BigDecimal amount) {
        this();
        this.referenceId = referenceId;
        this.merchant = merchant;
        this.amount = amount;
        calculateAmounts();
    }

    // Business Methods
    public boolean isPending() {
        return PaymentStatus.PENDING.equals(this.status);
    }

    public boolean isProcessing() {
        return PaymentStatus.PROCESSING.equals(this.status);
    }

    public boolean isApproved() {
        return PaymentStatus.APPROVED.equals(this.status);
    }

    public boolean isRejected() {
        return PaymentStatus.REJECTED.equals(this.status);
    }

    public boolean isCancelled() {
        return PaymentStatus.CANCELLED.equals(this.status);
    }

    public boolean isRefunded() {
        return PaymentStatus.REFUNDED.equals(this.status) || PaymentStatus.PARTIALLY_REFUNDED.equals(this.status);
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean canBeRefunded() {
        return isApproved() && (refundableAmount == null || refundableAmount.compareTo(BigDecimal.ZERO) > 0);
    }

    public void approve() {
        if (!isPending() && !isProcessing()) {
            throw new IllegalStateException("Payment can only be approved from PENDING or PROCESSING status");
        }
        this.status = PaymentStatus.APPROVED;
        this.processedAt = Instant.now();
        this.confirmedAt = Instant.now();
        this.refundableAmount = this.amount;
    }

    public void reject(String reason, String code) {
        if (!isPending() && !isProcessing()) {
            throw new IllegalStateException("Payment can only be rejected from PENDING or PROCESSING status");
        }
        this.status = PaymentStatus.REJECTED;
        this.failedAt = Instant.now();
        this.failureReason = reason;
        this.failureCode = code;
    }

    public void cancel() {
        if (isApproved()) {
            throw new IllegalStateException("Cannot cancel an approved payment. Use refund instead.");
        }
        this.status = PaymentStatus.CANCELLED;
        this.failedAt = Instant.now();
    }

    public void process() {
        if (!isPending()) {
            throw new IllegalStateException("Payment can only be processed from PENDING status");
        }
        this.status = PaymentStatus.PROCESSING;
        this.processedAt = Instant.now();
    }

    public void refund(BigDecimal refundAmount) {
        if (!canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded");
        }

        BigDecimal currentRefundable = refundableAmount != null ? refundableAmount : amount;
        if (refundAmount.compareTo(currentRefundable) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed refundable amount");
        }

        this.refundedAmount = this.refundedAmount.add(refundAmount);
        this.refundableAmount = currentRefundable.subtract(refundAmount);

        if (this.refundableAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.status = PaymentStatus.REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }

        this.refundedAt = Instant.now();
    }

    public void expire() {
        if (isPending() || isProcessing()) {
            this.status = PaymentStatus.EXPIRED;
            this.failedAt = Instant.now();
            this.failureReason = "Payment expired";
        }
    }

    private void calculateAmounts() {
        if (merchant != null && merchant.getFeeRate() != null && amount != null) {
            this.feeRate = merchant.getFeeRate();
            this.feeAmount = amount.multiply(feeRate).divide(BigDecimal.valueOf(100));
            this.netAmount = amount.subtract(feeAmount);
        }
    }

    public void updateMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
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
        calculateAmounts();
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        calculateAmounts();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(BigDecimal feeRate) {
        this.feeRate = feeRate;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(Instant refundedAt) {
        this.refundedAt = refundedAt;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getPixKey() {
        return pixKey;
    }

    public void setPixKey(String pixKey) {
        this.pixKey = pixKey;
    }

    public String getPixQrCode() {
        return pixQrCode;
    }

    public void setPixQrCode(String pixQrCode) {
        this.pixQrCode = pixQrCode;
    }

    public String getPixTransactionId() {
        return pixTransactionId;
    }

    public void setPixTransactionId(String pixTransactionId) {
        this.pixTransactionId = pixTransactionId;
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

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public BigDecimal getRefundableAmount() {
        return refundableAmount;
    }

    public void setRefundableAmount(BigDecimal refundableAmount) {
        this.refundableAmount = refundableAmount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public void setCustomerDocument(String customerDocument) {
        this.customerDocument = customerDocument;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }

    public void setNotificationUrl(String notificationUrl) {
        this.notificationUrl = notificationUrl;
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

    public Set<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(Set<Transaction> transactions) {
        this.transactions = transactions;
    }

    public Set<Webhook> getWebhooks() {
        return webhooks;
    }

    public void setWebhooks(Set<Webhook> webhooks) {
        this.webhooks = webhooks;
    }
}