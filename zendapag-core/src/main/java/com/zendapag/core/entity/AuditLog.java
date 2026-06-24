package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.AuditAction;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_log_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_log_user", columnList = "user_id"),
    @Index(name = "idx_audit_log_merchant", columnList = "merchant_id"),
    @Index(name = "idx_audit_log_action", columnList = "action"),
    @Index(name = "idx_audit_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_log_ip", columnList = "ip_address"),
    @Index(name = "idx_audit_log_session", columnList = "session_id"),
    @Index(name = "idx_audit_log_composite", columnList = "entity_type, entity_id, timestamp")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", foreignKey = @ForeignKey(name = "fk_audit_log_merchant"))
    private Merchant merchant;

    @NotNull(message = "Entity type is required")
    @Size(min = 1, max = 100, message = "Entity type must be between 1 and 100 characters")
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @NotNull(message = "Entity ID is required")
    @Size(min = 1, max = 255, message = "Entity ID must be between 1 and 255 characters")
    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @NotNull(message = "Action is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AuditAction action;

    @NotNull(message = "Timestamp is required")
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Size(max = 255, message = "User ID must be at most 255 characters")
    @Column(name = "user_id")
    private String userId;

    @Size(max = 255, message = "User name must be at most 255 characters")
    @Column(name = "user_name")
    private String userName;

    @Size(max = 100, message = "User type must be at most 100 characters")
    @Column(name = "user_type", length = 100)
    private String userType;

    // Request Information
    @Size(max = 45, message = "IP address must be at most 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 500, message = "User agent must be at most 500 characters")
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Size(max = 255, message = "Session ID must be at most 255 characters")
    @Column(name = "session_id")
    private String sessionId;

    @Size(max = 100, message = "Request method must be at most 100 characters")
    @Column(name = "request_method", length = 100)
    private String requestMethod;

    @Size(max = 1000, message = "Request URI must be at most 1000 characters")
    @Column(name = "request_uri", length = 1000)
    private String requestUri;

    // Change Information
    @Column(name = "description", length = 1000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values")
    private Map<String, Object> oldValues = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values")
    private Map<String, Object> newValues = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata = new HashMap<>();

    // Risk and Security
    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "suspicious", nullable = false)
    private Boolean suspicious = false;

    @Size(max = 1000, message = "Tags must be at most 1000 characters")
    @Column(name = "tags", length = 1000)
    private String tags;

    // External References
    @Size(max = 255, message = "Correlation ID must be at most 255 characters")
    @Column(name = "correlation_id")
    private String correlationId;

    @Size(max = 255, message = "Request ID must be at most 255 characters")
    @Column(name = "request_id")
    private String requestId;

    // API Information
    @Size(max = 255, message = "API key ID must be at most 255 characters")
    @Column(name = "api_key_id")
    private String apiKeyId;

    @Size(max = 100, message = "API version must be at most 100 characters")
    @Column(name = "api_version", length = 100)
    private String apiVersion;

    // Additional Context
    @Size(max = 255, message = "Application must be at most 255 characters")
    @Column(name = "application")
    private String application;

    @Size(max = 255, message = "Module must be at most 255 characters")
    @Column(name = "module")
    private String module;

    @Size(max = 255, message = "Feature must be at most 255 characters")
    @Column(name = "feature")
    private String feature;

    // Processing Information
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "success", nullable = false)
    private Boolean success = true;

    @Size(max = 1000, message = "Error message must be at most 1000 characters")
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    // Constructors
    public AuditLog() {
        this.timestamp = Instant.now();
        this.suspicious = false;
        this.success = true;
    }

    public AuditLog(String entityType, String entityId, AuditAction action) {
        this();
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
    }

    public AuditLog(Merchant merchant, String entityType, String entityId, AuditAction action) {
        this(entityType, entityId, action);
        this.merchant = merchant;
    }

    // Static Factory Methods
    public static AuditLog create(String entityType, String entityId, AuditAction action) {
        return new AuditLog(entityType, entityId, action);
    }

    public static AuditLog create(Merchant merchant, String entityType, String entityId, AuditAction action) {
        return new AuditLog(merchant, entityType, entityId, action);
    }

    // Business Methods
    public void addOldValue(String field, Object value) {
        if (oldValues == null) {
            oldValues = new HashMap<>();
        }
        oldValues.put(field, value);
    }

    public void addNewValue(String field, Object value) {
        if (newValues == null) {
            newValues = new HashMap<>();
        }
        newValues.put(field, value);
    }

    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    public void setUserContext(String userId, String userName, String userType) {
        this.userId = userId;
        this.userName = userName;
        this.userType = userType;
    }

    public void setRequestContext(String ipAddress, String userAgent, String sessionId, String method, String uri) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.sessionId = sessionId;
        this.requestMethod = method;
        this.requestUri = uri;
    }

    public void setApiContext(String apiKeyId, String apiVersion, String correlationId, String requestId) {
        this.apiKeyId = apiKeyId;
        this.apiVersion = apiVersion;
        this.correlationId = correlationId;
        this.requestId = requestId;
    }

    public void setApplicationContext(String application, String module, String feature) {
        this.application = application;
        this.module = module;
        this.feature = feature;
    }

    public void markSuspicious(String reason) {
        this.suspicious = true;
        addMetadata("suspicious_reason", reason);
    }

    public void markAsFailure(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
    }

    public void addTag(String tag) {
        if (tags == null) {
            tags = tag;
        } else {
            tags = tags + "," + tag;
        }
    }

    public boolean hasChanges() {
        return (oldValues != null && !oldValues.isEmpty()) ||
               (newValues != null && !newValues.isEmpty());
    }

    // Getters and Setters
    public java.util.UUID getId() {
        return id;
    }

    public void setId(java.util.UUID id) {
        this.id = id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(Map<String, Object> oldValues) {
        this.oldValues = oldValues;
    }

    public Map<String, Object> getNewValues() {
        return newValues;
    }

    public void setNewValues(Map<String, Object> newValues) {
        this.newValues = newValues;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public Boolean getSuspicious() {
        return suspicious;
    }

    public void setSuspicious(Boolean suspicious) {
        this.suspicious = suspicious;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", action=" + action +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", success=" + success +
                '}';
    }
}