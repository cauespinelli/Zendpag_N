package com.zendapag.core.pix.reconciliation;

import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.ReconciliationReport;
import com.zendapag.core.pix.client.PixClient;
import com.zendapag.core.pix.config.PixConfig;
import com.zendapag.core.pix.dto.PixPaymentResponse;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.repository.ReconciliationReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PixReconciliationService {

    private final PixClient pixClient;
    private final PixConfig pixConfig;
    private final PaymentRepository paymentRepository;
    private final ReconciliationReportRepository reconciliationReportRepository;

    @Autowired
    public PixReconciliationService(
            PixClient pixClient,
            PixConfig pixConfig,
            PaymentRepository paymentRepository,
            ReconciliationReportRepository reconciliationReportRepository) {
        this.pixClient = pixClient;
        this.pixConfig = pixConfig;
        this.paymentRepository = paymentRepository;
        this.reconciliationReportRepository = reconciliationReportRepository;
    }

    /**
     * Scheduled reconciliation job - runs daily at configured time
     */
    @Scheduled(cron = "#{@pixConfig.getReconciliation().getCronExpression()}")
    @Transactional
    public void performScheduledReconciliation() {
        log.info("Starting scheduled PIX reconciliation");

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            ReconciliationResult result = performReconciliation(yesterday, yesterday);

            log.info("Scheduled reconciliation completed. Processed: {}, Discrepancies: {}",
                    result.getTotalProcessed(), result.getDiscrepancies().size());

        } catch (Exception e) {
            log.error("Scheduled reconciliation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual reconciliation for specific date range
     */
    @Transactional
    public ReconciliationResult performReconciliation(LocalDate startDate, LocalDate endDate) {
        log.info("Starting reconciliation from {} to {}", startDate, endDate);

        ReconciliationResult result = new ReconciliationResult(startDate, endDate);

        try {
            // Create reconciliation report
            ReconciliationReport report = createReconciliationReport(startDate, endDate);

            // Get internal payments for date range
            List<Payment> internalPayments = getInternalPayments(startDate, endDate);
            result.setTotalInternal(internalPayments.size());

            // Get external payments from PIX participant
            List<PixPaymentResponse> externalPayments = getExternalPayments(startDate, endDate);
            result.setTotalExternal(externalPayments.size());

            // Compare payments and find discrepancies
            List<ReconciliationDiscrepancy> discrepancies = comparePayments(internalPayments, externalPayments);
            result.setDiscrepancies(discrepancies);

            // Calculate summary
            calculateSummary(result, internalPayments, externalPayments);

            // Update report
            updateReconciliationReport(report, result);

            // Handle discrepancies
            handleDiscrepancies(discrepancies);

            result.setSuccess(true);
            result.setCompletedAt(Instant.now());

            log.info("Reconciliation completed successfully for {} to {}", startDate, endDate);
            return result;

        } catch (Exception e) {
            log.error("Reconciliation failed for {} to {}: {}", startDate, endDate, e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setCompletedAt(Instant.now());
            return result;
        }
    }

    /**
     * Quick reconciliation for recent transactions
     */
    @Transactional
    public QuickReconciliationResult performQuickReconciliation(int hours) {
        log.info("Starting quick reconciliation for last {} hours", hours);

        Instant startTime = Instant.now().minusSeconds(hours * 3600L);
        Instant endTime = Instant.now();

        try {
            // Get recent internal payments
            List<Payment> internalPayments = paymentRepository.findByCreatedAtBetween(startTime, endTime);

            QuickReconciliationResult result = new QuickReconciliationResult();
            result.setStartTime(startTime);
            result.setEndTime(endTime);
            result.setTotalPayments(internalPayments.size());

            // Check status of each payment with external system
            int matchedPayments = 0;
            List<String> missingPayments = new ArrayList<>();

            for (Payment payment : internalPayments) {
                if (payment.getPixTxId() != null) {
                    try {
                        PixPaymentResponse externalPayment = pixClient.checkPaymentStatus(payment.getPixTxId());

                        if (paymentStatusMatches(payment, externalPayment)) {
                            matchedPayments++;
                        } else {
                            missingPayments.add(payment.getReferenceId() + " (status mismatch)");
                        }

                    } catch (Exception e) {
                        log.warn("Failed to check external status for payment {}: {}", payment.getReferenceId(), e.getMessage());
                        missingPayments.add(payment.getReferenceId() + " (check failed)");
                    }
                }
            }

            result.setMatchedPayments(matchedPayments);
            result.setMissingPayments(missingPayments);
            result.setSuccess(true);

            log.info("Quick reconciliation completed. Matched: {}, Missing: {}", matchedPayments, missingPayments.size());
            return result;

        } catch (Exception e) {
            log.error("Quick reconciliation failed: {}", e.getMessage(), e);

            QuickReconciliationResult result = new QuickReconciliationResult();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /**
     * Generate reconciliation report for a specific period
     */
    public ReconciliationSummary generateReconciliationSummary(LocalDate startDate, LocalDate endDate) {
        log.debug("Generating reconciliation summary from {} to {}", startDate, endDate);

        try {
            List<Payment> payments = getInternalPayments(startDate, endDate);

            ReconciliationSummary summary = new ReconciliationSummary();
            summary.setStartDate(startDate);
            summary.setEndDate(endDate);
            summary.setGeneratedAt(Instant.now());

            // Calculate metrics
            summary.setTotalTransactions(payments.size());
            summary.setCompletedTransactions((int) payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .count());

            summary.setTotalAmount(payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            summary.setFailedTransactions((int) payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.FAILED)
                    .count());

            summary.setCancelledTransactions((int) payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.CANCELLED)
                    .count());

            // Get recent reconciliation reports
            List<ReconciliationReport> reports = reconciliationReportRepository
                    .findByReportDateBetweenOrderByReportDateDesc(startDate, endDate);

            summary.setTotalDiscrepancies(reports.stream()
                    .mapToInt(ReconciliationReport::getDiscrepancyCount)
                    .sum());

            return summary;

        } catch (Exception e) {
            log.error("Failed to generate reconciliation summary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate reconciliation summary", e);
        }
    }

    private List<Payment> getInternalPayments(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        return paymentRepository.findByCreatedAtBetweenAndStatusIn(
                startInstant, endInstant,
                Arrays.asList(
                        Payment.PaymentStatus.COMPLETED,
                        Payment.PaymentStatus.FAILED,
                        Payment.PaymentStatus.CANCELLED
                )
        );
    }

    private List<PixPaymentResponse> getExternalPayments(LocalDate startDate, LocalDate endDate) {
        try {
            Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant endInstant = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

            // Get payments from external system in batches
            List<PixPaymentResponse> allPayments = new ArrayList<>();
            int page = 0;
            int size = pixConfig.getReconciliation().getBatchSize();

            PixPaymentResponse[] payments;
            do {
                payments = pixClient.listPayments(startInstant, endInstant, page, size);
                allPayments.addAll(Arrays.asList(payments));
                page++;
            } while (payments.length == size);

            log.debug("Retrieved {} external payments from {} to {}", allPayments.size(), startDate, endDate);
            return allPayments;

        } catch (Exception e) {
            log.error("Failed to retrieve external payments: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve external payments", e);
        }
    }

    private List<ReconciliationDiscrepancy> comparePayments(
            List<Payment> internalPayments,
            List<PixPaymentResponse> externalPayments) {

        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

        // Create maps for efficient lookup
        Map<String, Payment> internalByTxId = internalPayments.stream()
                .filter(p -> p.getPixTxId() != null)
                .collect(Collectors.toMap(Payment::getPixTxId, p -> p));

        Map<String, Payment> internalByRefId = internalPayments.stream()
                .collect(Collectors.toMap(Payment::getReferenceId, p -> p));

        Map<String, PixPaymentResponse> externalByTxId = externalPayments.stream()
                .collect(Collectors.toMap(PixPaymentResponse::getTxId, p -> p));

        // Check for missing external payments
        for (Payment internal : internalPayments) {
            if (internal.getPixTxId() != null) {
                PixPaymentResponse external = externalByTxId.get(internal.getPixTxId());

                if (external == null) {
                    discrepancies.add(new ReconciliationDiscrepancy(
                            ReconciliationDiscrepancy.Type.MISSING_EXTERNAL,
                            internal.getReferenceId(),
                            internal.getPixTxId(),
                            "Payment exists internally but not in external system",
                            internal.getAmount(),
                            null
                    ));
                } else {
                    // Check for data discrepancies
                    checkPaymentDiscrepancies(internal, external, discrepancies);
                }
            }
        }

        // Check for missing internal payments
        for (PixPaymentResponse external : externalPayments) {
            Payment internal = internalByTxId.get(external.getTxId());

            if (internal == null && external.getReferenceId() != null) {
                internal = internalByRefId.get(external.getReferenceId());
            }

            if (internal == null) {
                discrepancies.add(new ReconciliationDiscrepancy(
                        ReconciliationDiscrepancy.Type.MISSING_INTERNAL,
                        external.getReferenceId(),
                        external.getTxId(),
                        "Payment exists in external system but not internally",
                        null,
                        external.getAmount()
                ));
            }
        }

        return discrepancies;
    }

    private void checkPaymentDiscrepancies(Payment internal, PixPaymentResponse external, List<ReconciliationDiscrepancy> discrepancies) {
        // Check amount
        if (internal.getAmount().compareTo(external.getAmount()) != 0) {
            discrepancies.add(new ReconciliationDiscrepancy(
                    ReconciliationDiscrepancy.Type.AMOUNT_MISMATCH,
                    internal.getReferenceId(),
                    internal.getPixTxId(),
                    String.format("Amount mismatch: internal=%.2f, external=%.2f",
                            internal.getAmount(), external.getAmount()),
                    internal.getAmount(),
                    external.getAmount()
            ));
        }

        // Check status
        if (!paymentStatusMatches(internal, external)) {
            discrepancies.add(new ReconciliationDiscrepancy(
                    ReconciliationDiscrepancy.Type.STATUS_MISMATCH,
                    internal.getReferenceId(),
                    internal.getPixTxId(),
                    String.format("Status mismatch: internal=%s, external=%s",
                            internal.getStatus(), external.getStatus()),
                    internal.getAmount(),
                    external.getAmount()
            ));
        }
    }

    private boolean paymentStatusMatches(Payment internal, PixPaymentResponse external) {
        switch (internal.getStatus()) {
            case COMPLETED:
                return external.getStatus() == PixPaymentResponse.PixPaymentStatus.COMPLETED;
            case FAILED:
                return external.getStatus() == PixPaymentResponse.PixPaymentStatus.REJECTED ||
                       external.getStatus() == PixPaymentResponse.PixPaymentStatus.ERROR;
            case CANCELLED:
                return external.getStatus() == PixPaymentResponse.PixPaymentStatus.CANCELLED;
            case EXPIRED:
                return external.getStatus() == PixPaymentResponse.PixPaymentStatus.EXPIRED;
            default:
                return false;
        }
    }

    private void calculateSummary(ReconciliationResult result, List<Payment> internalPayments, List<PixPaymentResponse> externalPayments) {
        result.setTotalProcessed(Math.max(internalPayments.size(), externalPayments.size()));

        // Calculate amounts
        BigDecimal internalTotal = internalPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal externalTotal = externalPayments.stream()
                .filter(p -> p.getStatus() == PixPaymentResponse.PixPaymentStatus.COMPLETED)
                .map(PixPaymentResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.setInternalTotal(internalTotal);
        result.setExternalTotal(externalTotal);
        result.setAmountDifference(internalTotal.subtract(externalTotal));
    }

    private ReconciliationReport createReconciliationReport(LocalDate startDate, LocalDate endDate) {
        ReconciliationReport report = new ReconciliationReport();
        report.setReportDate(startDate);
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setStatus(ReconciliationReport.Status.PROCESSING);
        report.setCreatedAt(Instant.now());

        return reconciliationReportRepository.save(report);
    }

    private void updateReconciliationReport(ReconciliationReport report, ReconciliationResult result) {
        report.setStatus(result.isSuccess() ? ReconciliationReport.Status.COMPLETED : ReconciliationReport.Status.FAILED);
        report.setTotalInternal(result.getTotalInternal());
        report.setTotalExternal(result.getTotalExternal());
        report.setDiscrepancyCount(result.getDiscrepancies().size());
        report.setInternalTotal(result.getInternalTotal());
        report.setExternalTotal(result.getExternalTotal());
        report.setAmountDifference(result.getAmountDifference());
        report.setCompletedAt(result.getCompletedAt());

        if (!result.isSuccess()) {
            report.setErrorMessage(result.getErrorMessage());
        }

        reconciliationReportRepository.save(report);
    }

    private void handleDiscrepancies(List<ReconciliationDiscrepancy> discrepancies) {
        if (discrepancies.isEmpty()) {
            return;
        }

        log.warn("Found {} discrepancies in reconciliation", discrepancies.size());

        // Group discrepancies by type
        Map<ReconciliationDiscrepancy.Type, List<ReconciliationDiscrepancy>> grouped = discrepancies.stream()
                .collect(Collectors.groupingBy(ReconciliationDiscrepancy::getType));

        for (Map.Entry<ReconciliationDiscrepancy.Type, List<ReconciliationDiscrepancy>> entry : grouped.entrySet()) {
            log.warn("Discrepancy type {}: {} occurrences", entry.getKey(), entry.getValue().size());

            // In production, you might want to:
            // 1. Send alerts to operations team
            // 2. Create tickets for manual review
            // 3. Attempt automatic reconciliation for certain types
            // 4. Update payment status based on external truth
        }
    }

    // Result classes
    public static class ReconciliationResult {
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean success;
        private String errorMessage;
        private int totalInternal;
        private int totalExternal;
        private int totalProcessed;
        private BigDecimal internalTotal;
        private BigDecimal externalTotal;
        private BigDecimal amountDifference;
        private List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();
        private Instant completedAt;

        public ReconciliationResult(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        // Getters and setters
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getTotalInternal() { return totalInternal; }
        public void setTotalInternal(int totalInternal) { this.totalInternal = totalInternal; }
        public int getTotalExternal() { return totalExternal; }
        public void setTotalExternal(int totalExternal) { this.totalExternal = totalExternal; }
        public int getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
        public BigDecimal getInternalTotal() { return internalTotal; }
        public void setInternalTotal(BigDecimal internalTotal) { this.internalTotal = internalTotal; }
        public BigDecimal getExternalTotal() { return externalTotal; }
        public void setExternalTotal(BigDecimal externalTotal) { this.externalTotal = externalTotal; }
        public BigDecimal getAmountDifference() { return amountDifference; }
        public void setAmountDifference(BigDecimal amountDifference) { this.amountDifference = amountDifference; }
        public List<ReconciliationDiscrepancy> getDiscrepancies() { return discrepancies; }
        public void setDiscrepancies(List<ReconciliationDiscrepancy> discrepancies) { this.discrepancies = discrepancies; }
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    }

    public static class QuickReconciliationResult {
        private Instant startTime;
        private Instant endTime;
        private boolean success;
        private String errorMessage;
        private int totalPayments;
        private int matchedPayments;
        private List<String> missingPayments = new ArrayList<>();

        // Getters and setters
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getTotalPayments() { return totalPayments; }
        public void setTotalPayments(int totalPayments) { this.totalPayments = totalPayments; }
        public int getMatchedPayments() { return matchedPayments; }
        public void setMatchedPayments(int matchedPayments) { this.matchedPayments = matchedPayments; }
        public List<String> getMissingPayments() { return missingPayments; }
        public void setMissingPayments(List<String> missingPayments) { this.missingPayments = missingPayments; }
    }

    public static class ReconciliationSummary {
        private LocalDate startDate;
        private LocalDate endDate;
        private Instant generatedAt;
        private int totalTransactions;
        private int completedTransactions;
        private int failedTransactions;
        private int cancelledTransactions;
        private BigDecimal totalAmount;
        private int totalDiscrepancies;

        // Getters and setters
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public Instant getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
        public int getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
        public int getCompletedTransactions() { return completedTransactions; }
        public void setCompletedTransactions(int completedTransactions) { this.completedTransactions = completedTransactions; }
        public int getFailedTransactions() { return failedTransactions; }
        public void setFailedTransactions(int failedTransactions) { this.failedTransactions = failedTransactions; }
        public int getCancelledTransactions() { return cancelledTransactions; }
        public void setCancelledTransactions(int cancelledTransactions) { this.cancelledTransactions = cancelledTransactions; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public int getTotalDiscrepancies() { return totalDiscrepancies; }
        public void setTotalDiscrepancies(int totalDiscrepancies) { this.totalDiscrepancies = totalDiscrepancies; }
    }

    public static class ReconciliationDiscrepancy {
        private Type type;
        private String referenceId;
        private String txId;
        private String description;
        private BigDecimal internalAmount;
        private BigDecimal externalAmount;

        public enum Type {
            MISSING_INTERNAL,
            MISSING_EXTERNAL,
            AMOUNT_MISMATCH,
            STATUS_MISMATCH
        }

        public ReconciliationDiscrepancy(Type type, String referenceId, String txId, String description, BigDecimal internalAmount, BigDecimal externalAmount) {
            this.type = type;
            this.referenceId = referenceId;
            this.txId = txId;
            this.description = description;
            this.internalAmount = internalAmount;
            this.externalAmount = externalAmount;
        }

        // Getters and setters
        public Type getType() { return type; }
        public String getReferenceId() { return referenceId; }
        public String getTxId() { return txId; }
        public String getDescription() { return description; }
        public BigDecimal getInternalAmount() { return internalAmount; }
        public BigDecimal getExternalAmount() { return externalAmount; }
    }
}