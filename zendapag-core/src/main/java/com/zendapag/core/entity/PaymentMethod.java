package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.PaymentMethodType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_payment_method_merchant", columnList = "merchant_id"),
    @Index(name = "idx_payment_method_customer", columnList = "customer_id"),
    @Index(name = "idx_payment_method_type", columnList = "type"),
    @Index(name = "idx_payment_method_token", columnList = "token"),
    @Index(name = "idx_payment_method_brand", columnList = "brand"),
    @Index(name = "idx_payment_method_active", columnList = "active"),
    @Index(name = "idx_payment_method_created_at", columnList = "created_at"),
    @Index(name = "idx_payment_method_deleted", columnList = "deleted")
})
@SQLDelete(sql = "UPDATE payment_methods SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class PaymentMethod extends BaseEntity {

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_method_merchant"))
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "fk_payment_method_customer"))
    private Customer customer;

    @NotNull(message = "Type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PaymentMethodType type;

    @Size(max = 255, message = "Token must be at most 255 characters")
    @Column(name = "token")
    private String token;

    @Size(max = 100, message = "Name must be at most 100 characters")
    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // Card Information (encrypted/tokenized)
    @Size(max = 50, message = "Brand must be at most 50 characters")
    @Column(name = "brand", length = 50)
    private String brand;

    @Size(max = 4, message = "Last four digits must be 4 characters")
    @Column(name = "last_four", length = 4)
    private String lastFour;

    @Size(max = 6, message = "First six digits must be 6 characters")
    @Column(name = "first_six", length = 6)
    private String firstSix;

    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Column(name = "expiry_year")
    private Integer expiryYear;

    @Size(max = 100, message = "Holder name must be at most 100 characters")
    @Column(name = "holder_name", length = 100)
    private String holderName;

    @Size(max = 14, message = "Holder document must be at most 14 characters")
    @Column(name = "holder_document", length = 14)
    private String holderDocument;

    // PIX Information
    @Size(max = 255, message = "PIX key must be at most 255 characters")
    @Column(name = "pix_key")
    private String pixKey;

    @Size(max = 20, message = "PIX key type must be at most 20 characters")
    @Column(name = "pix_key_type", length = 20)
    private String pixKeyType;

    // Bank Information
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

    // Verification
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "verification_method", length = 50)
    private String verificationMethod;

    // Metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata = new HashMap<>();

    // External References
    @Size(max = 255, message = "External ID must be at most 255 characters")
    @Column(name = "external_id")
    private String externalId;

    @Size(max = 100, message = "Gateway must be at most 100 characters")
    @Column(name = "gateway", length = 100)
    private String gateway;

    @Size(max = 255, message = "Gateway token must be at most 255 characters")
    @Column(name = "gateway_token")
    private String gatewayToken;

    // Usage tracking
    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    // Relationships
    @OneToMany(mappedBy = "paymentMethod", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Payment> payments = new HashSet<>();

    // Constructors
    public PaymentMethod() {
        super();
        this.active = true;
        this.verified = false;
        this.usageCount = 0L;
    }

    public PaymentMethod(Merchant merchant, PaymentMethodType type) {
        this();
        this.merchant = merchant;
        this.type = type;
    }

    public PaymentMethod(Merchant merchant, Customer customer, PaymentMethodType type) {
        this(merchant, type);
        this.customer = customer;
    }

    // Business Methods
    public boolean isActive() {
        return Boolean.TRUE.equals(active) && !isDeleted();
    }

    public boolean canBeUsed() {
        return isActive() && (customer == null || customer.canMakePayments());
    }

    public boolean isExpired() {
        if (expiryMonth == null || expiryYear == null) {
            return false;
        }

        LocalDate now = LocalDate.now();
        LocalDate expiryDate = LocalDate.of(expiryYear, expiryMonth, 1).plusMonths(1).minusDays(1);
        return now.isAfter(expiryDate);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void verify(String method) {
        this.verified = true;
        this.verificationMethod = method;
    }

    public void incrementUsage() {
        this.usageCount++;
    }

    public boolean isCard() {
        return PaymentMethodType.CREDIT_CARD.equals(type) || PaymentMethodType.DEBIT_CARD.equals(type);
    }

    public boolean isPix() {
        return PaymentMethodType.PIX.equals(type);
    }

    public boolean isBankTransfer() {
        return PaymentMethodType.BANK_TRANSFER.equals(type);
    }

    public String getMaskedCardNumber() {
        if (!isCard() || firstSix == null || lastFour == null) {
            return null;
        }
        return firstSix + "******" + lastFour;
    }

    public void updateMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    // Getters and Setters
    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public PaymentMethodType getType() {
        return type;
    }

    public void setType(PaymentMethodType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getLastFour() {
        return lastFour;
    }

    public void setLastFour(String lastFour) {
        this.lastFour = lastFour;
    }

    public String getFirstSix() {
        return firstSix;
    }

    public void setFirstSix(String firstSix) {
        this.firstSix = firstSix;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Integer expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Integer expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public String getHolderDocument() {
        return holderDocument;
    }

    public void setHolderDocument(String holderDocument) {
        this.holderDocument = holderDocument;
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

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(String verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getGatewayToken() {
        return gatewayToken;
    }

    public void setGatewayToken(String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    public Long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }

    public Set<Payment> getPayments() {
        return payments;
    }

    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }
}