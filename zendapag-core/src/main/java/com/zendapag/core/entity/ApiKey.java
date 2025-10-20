package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.ApiKeyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_api_key_key_hash", columnList = "key_hash", unique = true),
    @Index(name = "idx_api_key_merchant", columnList = "merchant_id"),
    @Index(name = "idx_api_key_status", columnList = "status"),
    @Index(name = "idx_api_key_expires_at", columnList = "expires_at"),
    @Index(name = "idx_api_key_last_used", columnList = "last_used_at"),
    @Index(name = "idx_api_key_deleted", columnList = "deleted")
})
@SQLDelete(sql = "UPDATE api_keys SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class ApiKey extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Key hash is required")
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Size(max = 8, message = "Key prefix must be at most 8 characters")
    @Column(name = "key_prefix", length = 8)
    private String keyPrefix;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApiKeyStatus status;

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_api_key_merchant"))
    private Merchant merchant;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "last_used_ip", length = 45)
    private String lastUsedIp;

    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    // Permissions
    @Column(name = "can_read", nullable = false)
    private Boolean canRead = true;

    @Column(name = "can_write", nullable = false)
    private Boolean canWrite = true;

    @Column(name = "can_delete", nullable = false)
    private Boolean canDelete = false;

    // Rate limiting
    @Column(name = "rate_limit_rpm")
    private Integer rateLimitRpm; // Requests per minute

    @Column(name = "rate_limit_rph")
    private Integer rateLimitRph; // Requests per hour

    @Column(name = "rate_limit_rpd")
    private Integer rateLimitRpd; // Requests per day

    // IP restrictions
    @ElementCollection
    @CollectionTable(
        name = "api_key_allowed_ips",
        joinColumns = @JoinColumn(name = "api_key_id"),
        indexes = @Index(name = "idx_api_key_allowed_ips", columnList = "api_key_id")
    )
    @Column(name = "ip_address", length = 45)
    private Set<String> allowedIps = new HashSet<>();

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    // Constructors
    public ApiKey() {
        super();
        this.status = ApiKeyStatus.ACTIVE;
        this.usageCount = 0L;
    }

    public ApiKey(String name, String keyHash, Merchant merchant) {
        this();
        this.name = name;
        this.keyHash = keyHash;
        this.merchant = merchant;
    }

    // Business Methods
    public boolean isActive() {
        return ApiKeyStatus.ACTIVE.equals(this.status) && !isExpired() && !isDeleted();
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public void revoke() {
        this.status = ApiKeyStatus.REVOKED;
    }

    public void activate() {
        if (isExpired()) {
            this.status = ApiKeyStatus.EXPIRED;
        } else {
            this.status = ApiKeyStatus.ACTIVE;
        }
    }

    public void deactivate() {
        this.status = ApiKeyStatus.INACTIVE;
    }

    public void recordUsage(String ipAddress) {
        this.usageCount++;
        this.lastUsedAt = Instant.now();
        this.lastUsedIp = ipAddress;
    }

    public boolean isIpAllowed(String ipAddress) {
        return allowedIps.isEmpty() || allowedIps.contains(ipAddress);
    }

    public boolean hasPermission(String permission) {
        return switch (permission.toLowerCase()) {
            case "read" -> Boolean.TRUE.equals(canRead);
            case "write" -> Boolean.TRUE.equals(canWrite);
            case "delete" -> Boolean.TRUE.equals(canDelete);
            default -> false;
        };
    }

    public void setExpiration(Instant expiresAt) {
        this.expiresAt = expiresAt;
        if (isExpired()) {
            this.status = ApiKeyStatus.EXPIRED;
        }
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public ApiKeyStatus getStatus() {
        return status;
    }

    public void setStatus(ApiKeyStatus status) {
        this.status = status;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getLastUsedIp() {
        return lastUsedIp;
    }

    public void setLastUsedIp(String lastUsedIp) {
        this.lastUsedIp = lastUsedIp;
    }

    public Long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getCanRead() {
        return canRead;
    }

    public void setCanRead(Boolean canRead) {
        this.canRead = canRead;
    }

    public Boolean getCanWrite() {
        return canWrite;
    }

    public void setCanWrite(Boolean canWrite) {
        this.canWrite = canWrite;
    }

    public Boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }

    public Integer getRateLimitRpm() {
        return rateLimitRpm;
    }

    public void setRateLimitRpm(Integer rateLimitRpm) {
        this.rateLimitRpm = rateLimitRpm;
    }

    public Integer getRateLimitRph() {
        return rateLimitRph;
    }

    public void setRateLimitRph(Integer rateLimitRph) {
        this.rateLimitRph = rateLimitRph;
    }

    public Integer getRateLimitRpd() {
        return rateLimitRpd;
    }

    public void setRateLimitRpd(Integer rateLimitRpd) {
        this.rateLimitRpd = rateLimitRpd;
    }

    public Set<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(Set<String> allowedIps) {
        this.allowedIps = allowedIps;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}