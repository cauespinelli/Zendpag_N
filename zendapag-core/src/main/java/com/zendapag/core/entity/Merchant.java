package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.MerchantStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "merchants", indexes = {
    @Index(name = "idx_merchant_document", columnList = "document", unique = true),
    @Index(name = "idx_merchant_email", columnList = "email"),
    @Index(name = "idx_merchant_status", columnList = "status"),
    @Index(name = "idx_merchant_created_at", columnList = "created_at"),
    @Index(name = "idx_merchant_deleted", columnList = "deleted")
})
@SQLDelete(sql = "UPDATE merchants SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Merchant extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Document is required")
    @Pattern(regexp = "^[0-9]{11}$|^[0-9]{14}$", message = "Document must be a valid CPF (11 digits) or CNPJ (14 digits)")
    @Column(name = "document", nullable = false, unique = true, length = 14)
    private String document;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    @Column(name = "email", nullable = false)
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be a valid international format")
    @Size(max = 20, message = "Phone must be at most 20 characters")
    @Column(name = "phone", length = 20)
    private String phone;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MerchantStatus status;

    // Business Information
    @Size(max = 255, message = "Trading name must be at most 255 characters")
    @Column(name = "trading_name")
    private String tradingName;

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 255, message = "Website must be at most 255 characters")
    @Column(name = "website")
    private String website;

    // Address Information
    @Size(max = 10, message = "Postal code must be at most 10 characters")
    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Size(max = 255, message = "Address must be at most 255 characters")
    @Column(name = "address")
    private String address;

    @Size(max = 100, message = "City must be at most 100 characters")
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    @Column(name = "state", length = 100)
    private String state;

    @Size(max = 3, message = "Country code must be at most 3 characters")
    @Column(name = "country", length = 3)
    private String country;

    // Financial Information
    @DecimalMin(value = "0.0", inclusive = false, message = "Fee rate must be positive")
    @DecimalMax(value = "100.0", message = "Fee rate cannot exceed 100%")
    @Column(name = "fee_rate", precision = 5, scale = 4)
    private BigDecimal feeRate;

    @DecimalMin(value = "0.0", message = "Transaction limit must be non-negative")
    @Column(name = "transaction_limit", precision = 15, scale = 2)
    private BigDecimal transactionLimit;

    @DecimalMin(value = "0.0", message = "Daily limit must be non-negative")
    @Column(name = "daily_limit", precision = 15, scale = 2)
    private BigDecimal dailyLimit;

    @DecimalMin(value = "0.0", message = "Monthly limit must be non-negative")
    @Column(name = "monthly_limit", precision = 15, scale = 2)
    private BigDecimal monthlyLimit;

    // Origem (multi-tenant): "DIRETO" = estabelecimento próprio da Zend;
    // outras origens externas (ex.: ONE_A_ONE) registram via API Key.
    @Column(name = "source", nullable = false, length = 40)
    private String source = "DIRETO";

    // Id do estabelecimento no sistema da origem (mapeia os dois lados).
    @Column(name = "source_external_id", length = 120)
    private String sourceExternalId;

    // Settings
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;

    @Column(name = "notification_email")
    private String notificationEmail;

    @Column(name = "auto_settle", nullable = false)
    private Boolean autoSettle = true;

    @Column(name = "settlement_days")
    private Integer settlementDays = 1;

    // Compliance
    @Column(name = "kyc_verified", nullable = false)
    private Boolean kycVerified = false;

    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // Relationships
    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ApiKey> apiKeys = new HashSet<>();

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Payment> payments = new HashSet<>();

    // Constructors
    public Merchant() {
        super();
        this.status = MerchantStatus.PENDING_APPROVAL;
    }

    public Merchant(String name, String document, String email) {
        this();
        this.name = name;
        this.document = document;
        this.email = email;
    }

    // Business Methods
    public boolean isActive() {
        return MerchantStatus.ACTIVE.equals(this.status);
    }

    public boolean canProcessPayments() {
        return isActive() && Boolean.TRUE.equals(kycVerified) && !isDeleted();
    }

    public void activate() {
        if (Boolean.TRUE.equals(kycVerified)) {
            this.status = MerchantStatus.ACTIVE;
        } else {
            throw new IllegalStateException("Cannot activate merchant without KYC verification");
        }
    }

    public void suspend() {
        this.status = MerchantStatus.SUSPENDED;
    }

    public void block() {
        this.status = MerchantStatus.BLOCKED;
    }

    public void verifyKyc() {
        this.kycVerified = true;
        this.kycVerifiedAt = Instant.now();
    }

    public boolean isCpf() {
        return document != null && document.length() == 11;
    }

    public boolean isCnpj() {
        return document != null && document.length() == 14;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public void setStatus(MerchantStatus status) {
        this.status = status;
    }

    public String getTradingName() {
        return tradingName;
    }

    public void setTradingName(String tradingName) {
        this.tradingName = tradingName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public BigDecimal getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(BigDecimal feeRate) {
        this.feeRate = feeRate;
    }

    public BigDecimal getTransactionLimit() {
        return transactionLimit;
    }

    public void setTransactionLimit(BigDecimal transactionLimit) {
        this.transactionLimit = transactionLimit;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceExternalId() {
        return sourceExternalId;
    }

    public void setSourceExternalId(String sourceExternalId) {
        this.sourceExternalId = sourceExternalId;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public Boolean getAutoSettle() {
        return autoSettle;
    }

    public void setAutoSettle(Boolean autoSettle) {
        this.autoSettle = autoSettle;
    }

    public Integer getSettlementDays() {
        return settlementDays;
    }

    public void setSettlementDays(Integer settlementDays) {
        this.settlementDays = settlementDays;
    }

    public Boolean getKycVerified() {
        return kycVerified;
    }

    public void setKycVerified(Boolean kycVerified) {
        this.kycVerified = kycVerified;
    }

    public Instant getKycVerifiedAt() {
        return kycVerifiedAt;
    }

    public void setKycVerifiedAt(Instant kycVerifiedAt) {
        this.kycVerifiedAt = kycVerifiedAt;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
// Alias for phone field (compatibility with PixQrCodeGenerator)
    public String getPhoneNumber() {
        return phone;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phone = phoneNumber;
    }

    // Website URL alias
    public String getWebsiteUrl() {
        return website;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.website = websiteUrl;
    }

    // Address as Map (compatibility with PixQrCodeGenerator)
    public java.util.Map<String, Object> getAddressAsMap() {
        java.util.Map<String, Object> addressMap = new java.util.HashMap<>();
        if (address != null) addressMap.put("street", address);
        if (city != null) addressMap.put("city", city);
        if (state != null) addressMap.put("state", state);
        if (postalCode != null) addressMap.put("postalCode", postalCode);
        if (country != null) addressMap.put("country", country);
        return addressMap;
    }

    public Set<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(Set<ApiKey> apiKeys) {
        this.apiKeys = apiKeys;
    }

    public Set<Payment> getPayments() {
        return payments;
    }

    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }
}