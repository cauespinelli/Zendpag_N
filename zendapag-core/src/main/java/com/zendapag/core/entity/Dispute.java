package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.DisputeStatus;
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
@Table(name = "disputes", indexes = {
    @Index(name = "idx_dispute_merchant", columnList = "merchant_id"),
    @Index(name = "idx_dispute_payment", columnList = "payment_id"),
    @Index(name = "idx_dispute_status", columnList = "status"),
    @Index(name = "idx_dispute_reason_code", columnList = "reason_code"),
    @Index(name = "idx_dispute_amount", columnList = "dispute_amount"),
    @Index(name = "idx_dispute_due_date", columnList = "due_date"),
    @Index(name = "idx_dispute_opened_at", columnList = "opened_at"),
    @Index(name = "idx_dispute_resolved_at", columnList = "resolved_at"),
    @Index(name = "idx_dispute_created_at", columnList = "created_at"),
    @Index(name = "idx_dispute_deleted", columnList = "deleted")
})
@SQLDelete(sql = "UPDATE disputes SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Dispute extends BaseEntity {

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dispute_merchant"))
    private Merchant merchant;

    @NotNull(message = "Payment is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dispute_payment"))
    private Payment payment;

    @NotBlank(message = "External ID is required")
    @Size(max = 255, message = "External ID must be at most 255 characters")
    @Column(name = "external_id", nullable = false)
    private String externalId;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DisputeStatus status;

    @NotBlank(message = "Reason code is required")
    @Size(max = 50, message = "Reason code must be at most 50 characters")
    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Size(max = 500, message = "Reason description must be at most 500 characters")
    @Column(name = "reason_description", length = 500)
    private String reasonDescription;

    @NotNull(message = "Dispute amount is required")
    @DecimalMin(value = "0.01", message = "Dispute amount must be positive")
    @Column(name = "dispute_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal disputeAmount;

    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    // Timing Information
    @NotNull(message = "Opened date is required")
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    // Gateway Information
    @Size(max = 100, message = "Gateway must be at most 100 characters")
    @Column(name = "gateway", length = 100)
    private String gateway;

    @Size(max = 255, message = "Gateway dispute ID must be at most 255 characters")
    @Column(name = "gateway_dispute_id")
    private String gatewayDisputeId;

    @Size(max = 255, message = "Gateway case ID must be at most 255 characters")
    @Column(name = "gateway_case_id")
    private String gatewayCaseId;

    // Response Information
    @Column(name = "merchant_response", columnDefinition = "text")
    private String merchantResponse;

    @Column(name = "response_submitted_at")
    private Instant responseSubmittedAt;

    @Column(name = "response_due_date")
    private LocalDate responseDueDate;

    // Evidence Management
    @ElementCollection
    @CollectionTable(
        name = "dispute_evidence",
        joinColumns = @JoinColumn(name = "dispute_id"),
        indexes = @Index(name = "idx_dispute_evidence", columnList = "dispute_id")
    )
    @Column(name = "evidence_url", length = 500)
    private Set<String> evidenceUrls = new HashSet<>();

    @Column(name = "evidence_required", nullable = false)
    private Boolean evidenceRequired = true;

    // Financial Impact
    @Column(name = "liability_shift", nullable = false)
    private Boolean liabilityShift = false;

    @Column(name = "covered_by_protection", nullable = false)
    private Boolean coveredByProtection = false;

    @Column(name = "chargeback_fee", precision = 15, scale = 2)
    private BigDecimal chargebackFee;

    @Column(name = "recovery_amount", precision = 15, scale = 2)
    private BigDecimal recoveryAmount;

    // Communication
    @Column(name = "last_message", columnDefinition = "text")
    private String lastMessage;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_from", length = 100)
    private String lastMessageFrom;

    // Metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    // Constructors
    public Dispute() {
        super();
        this.status = DisputeStatus.OPENED;
        this.currency = "BRL";
        this.evidenceRequired = true;
        this.liabilityShift = false;
        this.coveredByProtection = false;
        this.openedAt = Instant.now();
    }

    public Dispute(Merchant merchant, Payment payment, String externalId, String reasonCode, BigDecimal disputeAmount) {
        this();
        this.merchant = merchant;
        this.payment = payment;
        this.externalId = externalId;
        this.reasonCode = reasonCode;
        this.disputeAmount = disputeAmount;
    }

    // Business Methods
    public boolean isOpen() {
        return DisputeStatus.OPENED.equals(this.status) ||
               DisputeStatus.IN_REVIEW.equals(this.status) ||
               DisputeStatus.UNDER_REVIEW.equals(this.status) ||
               DisputeStatus.WAITING_RESPONSE.equals(this.status);
    }

    public boolean isResolved() {
        return DisputeStatus.RESOLVED.equals(this.status) ||
               DisputeStatus.WON.equals(this.status) ||
               DisputeStatus.LOST.equals(this.status);
    }

    public boolean isExpired() {
        return DisputeStatus.EXPIRED.equals(this.status) ||
               (dueDate != null && LocalDate.now().isAfter(dueDate));
    }

    public boolean canSubmitResponse() {
        return isOpen() && !isExpired() &&
               (responseDueDate == null || !LocalDate.now().isAfter(responseDueDate));
    }

    public boolean requiresEvidence() {
        return Boolean.TRUE.equals(evidenceRequired);
    }

    public void submitResponse(String response) {
        if (!canSubmitResponse()) {
            throw new IllegalStateException("Cannot submit response for this dispute");
        }

        this.merchantResponse = response;
        this.responseSubmittedAt = Instant.now();

        if (DisputeStatus.OPENED.equals(this.status)) {
            this.status = DisputeStatus.IN_REVIEW;
        }
    }

    public void markUnderReview() {
        this.status = DisputeStatus.UNDER_REVIEW;
    }

    public void markWaitingResponse() {
        this.status = DisputeStatus.WAITING_RESPONSE;
    }

    public void resolve() {
        this.status = DisputeStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public void win() {
        this.status = DisputeStatus.WON;
        this.resolvedAt = Instant.now();
    }

    public void lose(BigDecimal recoveryAmount) {
        this.status = DisputeStatus.LOST;
        this.resolvedAt = Instant.now();
        this.recoveryAmount = recoveryAmount;
    }

    public void expire() {
        this.status = DisputeStatus.EXPIRED;
        this.expiredAt = Instant.now();
    }

    public void addEvidence(String evidenceUrl) {
        if (evidenceUrls == null) {
            evidenceUrls = new HashSet<>();
        }
        evidenceUrls.add(evidenceUrl);
    }

    public void addMessage(String message, String from) {
        this.lastMessage = message;
        this.lastMessageAt = Instant.now();
        this.lastMessageFrom = from;
    }

    public void updateMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    public long getDaysUntilDue() {
        if (dueDate == null) {
            return Long.MAX_VALUE;
        }
        return LocalDate.now().until(dueDate).getDays();
    }

    public long getDaysUntilResponseDue() {
        if (responseDueDate == null) {
            return Long.MAX_VALUE;
        }
        return LocalDate.now().until(responseDueDate).getDays();
    }

    // Getters and Setters
    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReasonDescription() {
        return reasonDescription;
    }

    public void setReasonDescription(String reasonDescription) {
        this.reasonDescription = reasonDescription;
    }

    public BigDecimal getDisputeAmount() {
        return disputeAmount;
    }

    public void setDisputeAmount(BigDecimal disputeAmount) {
        this.disputeAmount = disputeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getGatewayDisputeId() {
        return gatewayDisputeId;
    }

    public void setGatewayDisputeId(String gatewayDisputeId) {
        this.gatewayDisputeId = gatewayDisputeId;
    }

    public String getGatewayCaseId() {
        return gatewayCaseId;
    }

    public void setGatewayCaseId(String gatewayCaseId) {
        this.gatewayCaseId = gatewayCaseId;
    }

    public String getMerchantResponse() {
        return merchantResponse;
    }

    public void setMerchantResponse(String merchantResponse) {
        this.merchantResponse = merchantResponse;
    }

    public Instant getResponseSubmittedAt() {
        return responseSubmittedAt;
    }

    public void setResponseSubmittedAt(Instant responseSubmittedAt) {
        this.responseSubmittedAt = responseSubmittedAt;
    }

    public LocalDate getResponseDueDate() {
        return responseDueDate;
    }

    public void setResponseDueDate(LocalDate responseDueDate) {
        this.responseDueDate = responseDueDate;
    }

    public Set<String> getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(Set<String> evidenceUrls) {
        this.evidenceUrls = evidenceUrls;
    }

    public Boolean getEvidenceRequired() {
        return evidenceRequired;
    }

    public void setEvidenceRequired(Boolean evidenceRequired) {
        this.evidenceRequired = evidenceRequired;
    }

    public Boolean getLiabilityShift() {
        return liabilityShift;
    }

    public void setLiabilityShift(Boolean liabilityShift) {
        this.liabilityShift = liabilityShift;
    }

    public Boolean getCoveredByProtection() {
        return coveredByProtection;
    }

    public void setCoveredByProtection(Boolean coveredByProtection) {
        this.coveredByProtection = coveredByProtection;
    }

    public BigDecimal getChargebackFee() {
        return chargebackFee;
    }

    public void setChargebackFee(BigDecimal chargebackFee) {
        this.chargebackFee = chargebackFee;
    }

    public BigDecimal getRecoveryAmount() {
        return recoveryAmount;
    }

    public void setRecoveryAmount(BigDecimal recoveryAmount) {
        this.recoveryAmount = recoveryAmount;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastMessageFrom() {
        return lastMessageFrom;
    }

    public void setLastMessageFrom(String lastMessageFrom) {
        this.lastMessageFrom = lastMessageFrom;
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
}