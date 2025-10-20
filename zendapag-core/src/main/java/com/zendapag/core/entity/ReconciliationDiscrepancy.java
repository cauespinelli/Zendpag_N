package com.zendapag.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a reconciliation discrepancy
 * Contains details of transaction mismatches or missing transactions
 */
@Entity
@Table(name = "reconciliation_discrepancies", indexes = {
    @Index(name = "idx_discrepancy_type", columnList = "discrepancy_type"),
    @Index(name = "idx_discrepancy_status", columnList = "status"),
    @Index(name = "idx_discrepancy_transaction", columnList = "internal_transaction_id"),
    @Index(name = "idx_discrepancy_external", columnList = "external_transaction_id")
})
public class ReconciliationDiscrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_report_id", nullable = false)
    private ReconciliationReport reconciliationReport;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false)
    private DiscrepancyType discrepancyType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DiscrepancyStatus status = DiscrepancyStatus.PENDING;

    @Column(name = "internal_transaction_id")
    private UUID internalTransactionId;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "expected_amount", precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "actual_amount", precision = 19, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "amount_difference", precision = 19, scale = 2)
    private BigDecimal amountDifference;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolution_action")
    private String resolutionAction;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ReconciliationDiscrepancy() {}

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a missing internal transaction discrepancy
     */
    public static ReconciliationDiscrepancy missingInternal(String externalId, BigDecimal amount, UUID merchantId) {
        return builder()
            .discrepancyType(DiscrepancyType.MISSING_INTERNAL)
            .externalTransactionId(externalId)
            .actualAmount(amount)
            .merchantId(merchantId)
            .description("External transaction found but no corresponding internal transaction")
            .build();
    }

    /**
     * Create a missing external transaction discrepancy
     */
    public static ReconciliationDiscrepancy missingExternal(UUID internalId, BigDecimal amount, UUID merchantId) {
        return builder()
            .discrepancyType(DiscrepancyType.MISSING_EXTERNAL)
            .internalTransactionId(internalId)
            .expectedAmount(amount)
            .merchantId(merchantId)
            .description("Internal transaction found but no corresponding external transaction")
            .build();
    }

    /**
     * Create an amount mismatch discrepancy
     */
    public static ReconciliationDiscrepancy amountMismatch(UUID internalId, String externalId,
                                                          BigDecimal expectedAmount, BigDecimal actualAmount,
                                                          UUID merchantId) {
        return builder()
            .discrepancyType(DiscrepancyType.AMOUNT_MISMATCH)
            .internalTransactionId(internalId)
            .externalTransactionId(externalId)
            .expectedAmount(expectedAmount)
            .actualAmount(actualAmount)
            .amountDifference(actualAmount.subtract(expectedAmount))
            .merchantId(merchantId)
            .description(String.format("Amount mismatch: expected %s, actual %s", expectedAmount, actualAmount))
            .build();
    }

    /**
     * Create a status mismatch discrepancy
     */
    public static ReconciliationDiscrepancy statusMismatch(UUID internalId, String externalId,
                                                          String expectedStatus, String actualStatus,
                                                          UUID merchantId) {
        return builder()
            .discrepancyType(DiscrepancyType.STATUS_MISMATCH)
            .internalTransactionId(internalId)
            .externalTransactionId(externalId)
            .merchantId(merchantId)
            .description(String.format("Status mismatch: expected %s, actual %s", expectedStatus, actualStatus))
            .build();
    }

    /**
     * Mark discrepancy as resolved
     */
    public void markResolved(String resolvedBy, String resolutionAction, String notes) {
        this.status = DiscrepancyStatus.RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolutionAction = resolutionAction;
        this.resolutionNotes = notes;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Mark discrepancy as ignored
     */
    public void markIgnored(String resolvedBy, String reason) {
        this.status = DiscrepancyStatus.IGNORED;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = reason;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Check if discrepancy is monetary
     */
    public boolean isMonetaryDiscrepancy() {
        return discrepancyType == DiscrepancyType.AMOUNT_MISMATCH ||
               discrepancyType == DiscrepancyType.MISSING_INTERNAL ||
               discrepancyType == DiscrepancyType.MISSING_EXTERNAL;
    }

    /**
     * Get discrepancy severity
     */
    public DiscrepancySeverity getSeverity() {
        if (amountDifference != null) {
            BigDecimal absAmount = amountDifference.abs();
            if (absAmount.compareTo(new BigDecimal("1000.00")) >= 0) {
                return DiscrepancySeverity.HIGH;
            } else if (absAmount.compareTo(new BigDecimal("100.00")) >= 0) {
                return DiscrepancySeverity.MEDIUM;
            }
        }
        return DiscrepancySeverity.LOW;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ReconciliationReport getReconciliationReport() { return reconciliationReport; }
    public void setReconciliationReport(ReconciliationReport reconciliationReport) { this.reconciliationReport = reconciliationReport; }

    public DiscrepancyType getDiscrepancyType() { return discrepancyType; }
    public void setDiscrepancyType(DiscrepancyType discrepancyType) { this.discrepancyType = discrepancyType; }

    public DiscrepancyStatus getStatus() { return status; }
    public void setStatus(DiscrepancyStatus status) { this.status = status; }

    public UUID getInternalTransactionId() { return internalTransactionId; }
    public void setInternalTransactionId(UUID internalTransactionId) { this.internalTransactionId = internalTransactionId; }

    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

    public BigDecimal getAmountDifference() { return amountDifference; }
    public void setAmountDifference(BigDecimal amountDifference) { this.amountDifference = amountDifference; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getResolutionAction() { return resolutionAction; }
    public void setResolutionAction(String resolutionAction) { this.resolutionAction = resolutionAction; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Builder pattern for ReconciliationDiscrepancy
     */
    public static class Builder {
        private ReconciliationDiscrepancy discrepancy = new ReconciliationDiscrepancy();

        public Builder reconciliationReport(ReconciliationReport report) {
            discrepancy.reconciliationReport = report;
            return this;
        }

        public Builder discrepancyType(DiscrepancyType type) {
            discrepancy.discrepancyType = type;
            return this;
        }

        public Builder status(DiscrepancyStatus status) {
            discrepancy.status = status;
            return this;
        }

        public Builder internalTransactionId(UUID id) {
            discrepancy.internalTransactionId = id;
            return this;
        }

        public Builder externalTransactionId(String id) {
            discrepancy.externalTransactionId = id;
            return this;
        }

        public Builder merchantId(UUID merchantId) {
            discrepancy.merchantId = merchantId;
            return this;
        }

        public Builder expectedAmount(BigDecimal amount) {
            discrepancy.expectedAmount = amount;
            return this;
        }

        public Builder actualAmount(BigDecimal amount) {
            discrepancy.actualAmount = amount;
            return this;
        }

        public Builder amountDifference(BigDecimal difference) {
            discrepancy.amountDifference = difference;
            return this;
        }

        public Builder description(String description) {
            discrepancy.description = description;
            return this;
        }

        public Builder resolutionNotes(String notes) {
            discrepancy.resolutionNotes = notes;
            return this;
        }

        public Builder resolutionAction(String action) {
            discrepancy.resolutionAction = action;
            return this;
        }

        public ReconciliationDiscrepancy build() {
            return discrepancy;
        }
    }

    /**
     * Discrepancy type enumeration
     */
    public enum DiscrepancyType {
        MISSING_INTERNAL("Missing Internal Transaction"),
        MISSING_EXTERNAL("Missing External Transaction"),
        AMOUNT_MISMATCH("Amount Mismatch"),
        STATUS_MISMATCH("Status Mismatch"),
        DUPLICATE_TRANSACTION("Duplicate Transaction"),
        TIMING_DISCREPANCY("Timing Discrepancy");

        private final String description;

        DiscrepancyType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Discrepancy status enumeration
     */
    public enum DiscrepancyStatus {
        PENDING("Pending Review"),
        UNDER_INVESTIGATION("Under Investigation"),
        RESOLVED("Resolved"),
        IGNORED("Ignored"),
        ESCALATED("Escalated");

        private final String description;

        DiscrepancyStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Discrepancy severity enumeration
     */
    public enum DiscrepancySeverity {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        CRITICAL("Critical");

        private final String description;

        DiscrepancySeverity(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}