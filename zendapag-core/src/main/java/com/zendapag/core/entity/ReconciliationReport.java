package com.zendapag.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a daily reconciliation report
 * Contains summary of transaction matching and discrepancies
 */
@Entity
@Table(name = "reconciliation_reports", indexes = {
    @Index(name = "idx_reconciliation_date", columnList = "reconciliation_date"),
    @Index(name = "idx_reconciliation_status", columnList = "status"),
    @Index(name = "idx_reconciliation_created", columnList = "created_at")
})
public class ReconciliationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReconciliationStatus status;

    @Column(name = "internal_transaction_count")
    private Integer internalTransactionCount = 0;

    @Column(name = "external_transaction_count")
    private Integer externalTransactionCount = 0;

    @Column(name = "matched_transaction_count")
    private Integer matchedTransactionCount = 0;

    @Column(name = "unmatched_internal_count")
    private Integer unmatchedInternalCount = 0;

    @Column(name = "unmatched_external_count")
    private Integer unmatchedExternalCount = 0;

    @Column(name = "discrepancy_count")
    private Integer discrepancyCount = 0;

    @Column(name = "total_internal_amount", precision = 19, scale = 2)
    private BigDecimal totalInternalAmount = BigDecimal.ZERO;

    @Column(name = "total_external_amount", precision = 19, scale = 2)
    private BigDecimal totalExternalAmount = BigDecimal.ZERO;

    @Column(name = "amount_difference", precision = 19, scale = 2)
    private BigDecimal amountDifference = BigDecimal.ZERO;

    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;

    @Column(name = "reconciliation_notes", columnDefinition = "TEXT")
    private String reconciliationNotes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "reconciliationReport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy = "SYSTEM";

    public ReconciliationReport() {}

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Check if report has discrepancies
     */
    public boolean hasDiscrepancies() {
        return discrepancyCount != null && discrepancyCount > 0;
    }

    /**
     * Check if amounts are balanced
     */
    public boolean isBalanced() {
        return amountDifference != null &&
               amountDifference.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Calculate match percentage
     */
    public double getMatchPercentage() {
        if (internalTransactionCount == null || internalTransactionCount == 0) {
            return 0.0;
        }
        return (matchedTransactionCount != null ? matchedTransactionCount.doubleValue() : 0.0)
               / internalTransactionCount.doubleValue() * 100.0;
    }

    /**
     * Get reconciliation summary
     */
    public String getSummary() {
        return String.format("Date: %s, Status: %s, Matched: %d/%d (%.1f%%), Discrepancies: %d",
            reconciliationDate,
            status,
            matchedTransactionCount != null ? matchedTransactionCount : 0,
            internalTransactionCount != null ? internalTransactionCount : 0,
            getMatchPercentage(),
            discrepancyCount != null ? discrepancyCount : 0);
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDate getReconciliationDate() { return reconciliationDate; }
    public void setReconciliationDate(LocalDate reconciliationDate) { this.reconciliationDate = reconciliationDate; }

    public ReconciliationStatus getStatus() { return status; }
    public void setStatus(ReconciliationStatus status) { this.status = status; }

    public Integer getInternalTransactionCount() { return internalTransactionCount; }
    public void setInternalTransactionCount(Integer internalTransactionCount) { this.internalTransactionCount = internalTransactionCount; }

    public Integer getExternalTransactionCount() { return externalTransactionCount; }
    public void setExternalTransactionCount(Integer externalTransactionCount) { this.externalTransactionCount = externalTransactionCount; }

    public Integer getMatchedTransactionCount() { return matchedTransactionCount; }
    public void setMatchedTransactionCount(Integer matchedTransactionCount) { this.matchedTransactionCount = matchedTransactionCount; }

    public Integer getUnmatchedInternalCount() { return unmatchedInternalCount; }
    public void setUnmatchedInternalCount(Integer unmatchedInternalCount) { this.unmatchedInternalCount = unmatchedInternalCount; }

    public Integer getUnmatchedExternalCount() { return unmatchedExternalCount; }
    public void setUnmatchedExternalCount(Integer unmatchedExternalCount) { this.unmatchedExternalCount = unmatchedExternalCount; }

    public Integer getDiscrepancyCount() { return discrepancyCount; }
    public void setDiscrepancyCount(Integer discrepancyCount) { this.discrepancyCount = discrepancyCount; }

    public BigDecimal getTotalInternalAmount() { return totalInternalAmount; }
    public void setTotalInternalAmount(BigDecimal totalInternalAmount) { this.totalInternalAmount = totalInternalAmount; }

    public BigDecimal getTotalExternalAmount() { return totalExternalAmount; }
    public void setTotalExternalAmount(BigDecimal totalExternalAmount) { this.totalExternalAmount = totalExternalAmount; }

    public BigDecimal getAmountDifference() { return amountDifference; }
    public void setAmountDifference(BigDecimal amountDifference) { this.amountDifference = amountDifference; }

    public Long getProcessingDurationMs() { return processingDurationMs; }
    public void setProcessingDurationMs(Long processingDurationMs) { this.processingDurationMs = processingDurationMs; }

    public String getReconciliationNotes() { return reconciliationNotes; }
    public void setReconciliationNotes(String reconciliationNotes) { this.reconciliationNotes = reconciliationNotes; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<ReconciliationDiscrepancy> getDiscrepancies() { return discrepancies; }
    public void setDiscrepancies(List<ReconciliationDiscrepancy> discrepancies) { this.discrepancies = discrepancies; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    /**
     * Builder pattern for ReconciliationReport
     */
    public static class Builder {
        private ReconciliationReport report = new ReconciliationReport();

        public Builder reconciliationDate(LocalDate date) {
            report.reconciliationDate = date;
            return this;
        }

        public Builder status(ReconciliationStatus status) {
            report.status = status;
            return this;
        }

        public Builder internalTransactionCount(int count) {
            report.internalTransactionCount = count;
            return this;
        }

        public Builder externalTransactionCount(int count) {
            report.externalTransactionCount = count;
            return this;
        }

        public Builder matchedTransactionCount(int count) {
            report.matchedTransactionCount = count;
            return this;
        }

        public Builder unmatchedInternalCount(int count) {
            report.unmatchedInternalCount = count;
            return this;
        }

        public Builder unmatchedExternalCount(int count) {
            report.unmatchedExternalCount = count;
            return this;
        }

        public Builder discrepancyCount(int count) {
            report.discrepancyCount = count;
            return this;
        }

        public Builder totalInternalAmount(BigDecimal amount) {
            report.totalInternalAmount = amount;
            return this;
        }

        public Builder totalExternalAmount(BigDecimal amount) {
            report.totalExternalAmount = amount;
            return this;
        }

        public Builder amountDifference(BigDecimal difference) {
            report.amountDifference = difference;
            return this;
        }

        public Builder processingDurationMs(long duration) {
            report.processingDurationMs = duration;
            return this;
        }

        public Builder reconciliationNotes(String notes) {
            report.reconciliationNotes = notes;
            return this;
        }

        public Builder errorMessage(String error) {
            report.errorMessage = error;
            return this;
        }

        public Builder discrepancies(List<ReconciliationDiscrepancy> discrepancies) {
            report.discrepancies = discrepancies;
            return this;
        }

        public Builder createdBy(String createdBy) {
            report.createdBy = createdBy;
            return this;
        }

        public ReconciliationReport build() {
            return report;
        }
    }

    /**
     * Reconciliation status enumeration
     */
    public enum ReconciliationStatus {
        IN_PROGRESS("In Progress"),
        COMPLETED("Completed"),
        COMPLETED_WITH_DISCREPANCIES("Completed with Discrepancies"),
        FAILED("Failed"),
        REQUIRES_MANUAL_REVIEW("Requires Manual Review");

        private final String description;

        ReconciliationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}