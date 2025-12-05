package com.zendapag.core.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationResult {

    private LocalDate reconciliationDate;
    private Instant startedAt;
    private Instant completedAt;
    private ReconciliationStatus status;
    private int totalRecordsProcessed;
    private int matchedRecords;
    private int unmatchedRecords;
    private int discrepancyRecords;
    private BigDecimal totalMatchedAmount;
    private BigDecimal totalUnmatchedAmount;
    private BigDecimal totalDiscrepancyAmount;
    private List<ReconciliationMatch> matches;
    private List<ReconciliationDiscrepancy> discrepancies;
    private String errorMessage;

    public ReconciliationResult() {
        this.matches = new ArrayList<>();
        this.discrepancies = new ArrayList<>();
        this.totalMatchedAmount = BigDecimal.ZERO;
        this.totalUnmatchedAmount = BigDecimal.ZERO;
        this.totalDiscrepancyAmount = BigDecimal.ZERO;
    }

    public static ReconciliationResult success(LocalDate date, int total, int matched, int unmatched, int discrepancies) {
        ReconciliationResult result = new ReconciliationResult();
        result.setReconciliationDate(date);
        result.setStatus(ReconciliationStatus.COMPLETED);
        result.setTotalRecordsProcessed(total);
        result.setMatchedRecords(matched);
        result.setUnmatchedRecords(unmatched);
        result.setDiscrepancyRecords(discrepancies);
        result.setCompletedAt(Instant.now());
        return result;
    }

    public static ReconciliationResult failure(LocalDate date, String errorMessage) {
        ReconciliationResult result = new ReconciliationResult();
        result.setReconciliationDate(date);
        result.setStatus(ReconciliationStatus.FAILED);
        result.setErrorMessage(errorMessage);
        result.setCompletedAt(Instant.now());
        return result;
    }

    public enum ReconciliationStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIALLY_COMPLETED
    }

    // Getters and Setters
    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }

    public int getTotalRecordsProcessed() {
        return totalRecordsProcessed;
    }

    public void setTotalRecordsProcessed(int totalRecordsProcessed) {
        this.totalRecordsProcessed = totalRecordsProcessed;
    }

    public int getMatchedRecords() {
        return matchedRecords;
    }

    public void setMatchedRecords(int matchedRecords) {
        this.matchedRecords = matchedRecords;
    }

    public int getUnmatchedRecords() {
        return unmatchedRecords;
    }

    public void setUnmatchedRecords(int unmatchedRecords) {
        this.unmatchedRecords = unmatchedRecords;
    }

    public int getDiscrepancyRecords() {
        return discrepancyRecords;
    }

    public void setDiscrepancyRecords(int discrepancyRecords) {
        this.discrepancyRecords = discrepancyRecords;
    }

    public BigDecimal getTotalMatchedAmount() {
        return totalMatchedAmount;
    }

    public void setTotalMatchedAmount(BigDecimal totalMatchedAmount) {
        this.totalMatchedAmount = totalMatchedAmount;
    }

    public BigDecimal getTotalUnmatchedAmount() {
        return totalUnmatchedAmount;
    }

    public void setTotalUnmatchedAmount(BigDecimal totalUnmatchedAmount) {
        this.totalUnmatchedAmount = totalUnmatchedAmount;
    }

    public BigDecimal getTotalDiscrepancyAmount() {
        return totalDiscrepancyAmount;
    }

    public void setTotalDiscrepancyAmount(BigDecimal totalDiscrepancyAmount) {
        this.totalDiscrepancyAmount = totalDiscrepancyAmount;
    }

    public List<ReconciliationMatch> getMatches() {
        return matches;
    }

    public void setMatches(List<ReconciliationMatch> matches) {
        this.matches = matches;
    }

    public List<ReconciliationDiscrepancy> getDiscrepancies() {
        return discrepancies;
    }

    public void setDiscrepancies(List<ReconciliationDiscrepancy> discrepancies) {
        this.discrepancies = discrepancies;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void addMatch(ReconciliationMatch match) {
        this.matches.add(match);
        this.matchedRecords++;
        this.totalMatchedAmount = this.totalMatchedAmount.add(match.getAmount());
    }

    public void addDiscrepancy(ReconciliationDiscrepancy discrepancy) {
        this.discrepancies.add(discrepancy);
        this.discrepancyRecords++;
        this.totalDiscrepancyAmount = this.totalDiscrepancyAmount.add(discrepancy.getDifferenceAmount().abs());
    }

    public static class ReconciliationDiscrepancy {
        private String paymentId;
        private String externalId;
        private BigDecimal expectedAmount;
        private BigDecimal actualAmount;
        private BigDecimal differenceAmount;
        private String discrepancyType;
        private String description;

        public ReconciliationDiscrepancy() {}

        public ReconciliationDiscrepancy(String paymentId, String externalId, BigDecimal expectedAmount,
                                         BigDecimal actualAmount, String discrepancyType) {
            this.paymentId = paymentId;
            this.externalId = externalId;
            this.expectedAmount = expectedAmount;
            this.actualAmount = actualAmount;
            this.differenceAmount = expectedAmount.subtract(actualAmount);
            this.discrepancyType = discrepancyType;
        }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }

        public BigDecimal getExpectedAmount() { return expectedAmount; }
        public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }

        public BigDecimal getActualAmount() { return actualAmount; }
        public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

        public BigDecimal getDifferenceAmount() { return differenceAmount; }
        public void setDifferenceAmount(BigDecimal differenceAmount) { this.differenceAmount = differenceAmount; }

        public String getDiscrepancyType() { return discrepancyType; }
        public void setDiscrepancyType(String discrepancyType) { this.discrepancyType = discrepancyType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
