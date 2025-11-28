package com.zendapag.core.service;

import com.zendapag.core.dto.PixTransaction;
import com.zendapag.core.entity.ReconciliationDiscrepancy;
import com.zendapag.core.entity.ReconciliationReport;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.repository.ReconciliationReportRepository;
import com.zendapag.core.repository.TransactionRepository;
import com.zendapag.integration.pix.PixClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for performing daily reconciliation between internal and external PIX transactions
 * Automatically runs daily at 2 AM and handles discrepancies
 */
@Service
public class ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ReconciliationReportRepository reconciliationReportRepository;

    @Autowired
    private PixClient pixClient;

    @Autowired
    private ReconciliationMatcher reconciliationMatcher;

    @Autowired
    private AlertingService alertingService;

    @Autowired
    private ReconciliationDiscrepancyHandler discrepancyHandler;

    /**
     * Perform daily reconciliation at 2 AM
     */
    @Scheduled
    @Transactional
    public void performDailyReconciliation {
        LocalDate yesterday = LocalDate.now.minusDays(1);
        performReconciliation;
    }

    /**
     * Perform reconciliation for a specific date
     */
    @Transactional
    public ReconciliationReport performReconciliation {
        logger.info;

        long startTime = System.currentTimeMillis;
        ReconciliationReport report = null;

        try {
            // Check if reconciliation already exists for this date
            if ) {
                logger.warn;
                return reconciliationReportRepository.findByReconciliationDate.orElse(null);
            }

            // Create initial report
            report = ReconciliationReport.builder
                .reconciliationDate
                .status
                .createdBy
                .build;

            report = reconciliationReportRepository.save;

            // Perform the actual reconciliation
            ReconciliationResult result = reconcileTransactions;

            // Update report with results
            updateReportWithResults;

            // Calculate processing time
            long processingTime = System.currentTimeMillis - startTime;
            report.setProcessingDurationMs;

            // Determine final status
            if ) {
                report.setStatus;

                // Handle discrepancies
                handleDiscrepancies);

                // Send alerts if needed
                if ) {
                    alertingService.sendReconciliationAlert;
                }

            } else {
                report.setStatus;
            }

            report = reconciliationReportRepository.save;

            logger.info("Reconciliation completed for date: {} - Status: {}, Discrepancies: {}, Duration: {}ms",
                date, report.getStatus, report.getDiscrepancyCount(), processingTime);

            return report;

        } catch  {
            logger.error;

            if  {
                report.setStatus;
                report.setErrorMessage);
                report.setProcessingDurationMs - startTime);
                reconciliationReportRepository.save;
            }

            // Send critical alert
            alertingService.sendCriticalAlert;
            throw new ReconciliationException;
        }
    }

    /**
     * Reconcile transactions for a specific date
     */
    private ReconciliationResult reconcileTransactions {
        logger.debug;

        // 1. Get internal transactions
        List<Transaction> internalTransactions = transactionRepository.findByCreatedAtDate;
        logger.debug);

        // 2. Get external transactions from PIX participant
        List<PixTransaction> externalTransactions = fetchExternalTransactions;
        logger.debug);

        // 3. Perform matching
        return reconciliationMatcher.match;
    }

    /**
     * Fetch external transactions from PIX participant
     */
    private List<PixTransaction> fetchExternalTransactions {
        try {
            List<PixTransaction> transactions = pixClient.getTransactions;

            // Filter only settled transactions for reconciliation
            return transactions.stream
                .filter
                .collect);

        } catch  {
            logger.error;
            throw new ReconciliationException;
        }
    }

    /**
     * Update report with reconciliation results
     */
    private void updateReportWithResults {
        report.setInternalTransactionCount);
        report.setExternalTransactionCount);
        report.setMatchedTransactionCount);
        report.setUnmatchedInternalCount);
        report.setUnmatchedExternalCount);
        report.setDiscrepancyCount);

        // Calculate amounts
        report.setTotalInternalAmount);
        report.setTotalExternalAmount);
        report.setAmountDifference);

        // Add summary notes
        report.setReconciliationNotes);
    }

    /**
     * Handle discovered discrepancies
     */
    private void handleDiscrepancies {
        logger.info, report.getId());

        for  {
            discrepancy.setReconciliationReport;

            try {
                // Process each discrepancy based on its type
                discrepancyHandler.processDiscrepancy;

            } catch  {
                logger.error, e);
                discrepancy.setStatus;
            }
        }

        // Update report discrepancies
        report.setDiscrepancies;
    }

    /**
     * Check if reconciliation result requires alerting
     */
    private boolean requiresAlert {
        // Send alert if:
        // 1. There are high-value discrepancies
        // 2. Too many discrepancies
        // 3. Large amount difference

        if  > 50) {
            return true;
        }

        if .abs().compareTo(new BigDecimal("10000.00")) > 0) {
            return true;
        }

        // Check for high-value individual discrepancies
        return result.getDiscrepancies.stream()
            .anyMatch != null &&
                          d.getAmountDifference.abs().compareTo(new BigDecimal("5000.00")) > 0);
    }

    /**
     * Generate reconciliation notes
     */
    private String generateReconciliationNotes {
        StringBuilder notes = new StringBuilder;

        notes.append);
        notes.append));
        notes.append));
        notes.append));
        notes.append));

        if ) {
            notes.append));

            // Count discrepancies by type
            Map<ReconciliationDiscrepancy.DiscrepancyType, Long> discrepancyTypeCounts =
                result.getDiscrepancies.stream()
                    .collect(Collectors.groupingBy(
                        ReconciliationDiscrepancy::getDiscrepancyType,
                        Collectors.counting
                    ));

            discrepancyTypeCounts.forEach ->
                notes.append, count))
            );
        }

        return notes.toString;
    }

    /**
     * Get reconciliation report by date
     */
    public ReconciliationReport getReconciliationReport {
        return reconciliationReportRepository.findByReconciliationDate
            .orElse;
    }

    /**
     * Get reconciliation reports for date range
     */
    public List<ReconciliationReport> getReconciliationReports {
        return reconciliationReportRepository.findByReconciliationDateBetweenOrderByReconciliationDateDesc(
            startDate, endDate);
    }

    /**
     * Get reports with unresolved discrepancies
     */
    public List<ReconciliationReport> getReportsWithUnresolvedDiscrepancies {
        return reconciliationReportRepository.findByStatusInOrderByReconciliationDateDesc(
            List.of(
                ReconciliationReport.ReconciliationStatus.COMPLETED_WITH_DISCREPANCIES,
                ReconciliationReport.ReconciliationStatus.REQUIRES_MANUAL_REVIEW
            )
        );
    }

    /**
     * Manual reconciliation trigger for specific date
     */
    @Transactional
    public ReconciliationReport triggerManualReconciliation {
        logger.info;

        ReconciliationReport report = performReconciliation;
        report.setCreatedBy;

        return reconciliationReportRepository.save;
    }

    /**
     * Get reconciliation statistics
     */
    public ReconciliationStatistics getReconciliationStatistics {
        Object stats = reconciliationReportRepository.getReconciliationStatistics;
        Double successRate = reconciliationReportRepository.getSuccessRatePercentage;
        Double avgProcessingTime = reconciliationReportRepository.getAverageProcessingTime;

        return ReconciliationStatistics.builder
            .statistics
            .successRate
            .averageProcessingTimeMs
            .build;
    }

    /**
     * Custom exception for reconciliation errors
     */
    public static class ReconciliationException extends RuntimeException {
        public ReconciliationException {
            super;
        }

        public ReconciliationException {
            super;
        }
    }

    /**
     * Reconciliation statistics DTO
     */
    public static class ReconciliationStatistics {
        private Object statistics;
        private Double successRate;
        private Double averageProcessingTimeMs;

        public static Builder builder {
            return new Builder;
        }

        public Object getStatistics { return statistics; }
        public Double getSuccessRate { return successRate; }
        public Double getAverageProcessingTimeMs { return averageProcessingTimeMs; }

        public static class Builder {
            private ReconciliationStatistics stats = new ReconciliationStatistics;

            public Builder statistics {
                stats.statistics = statistics;
                return this;
            }

            public Builder successRate {
                stats.successRate = successRate;
                return this;
            }

            public Builder averageProcessingTimeMs {
                stats.averageProcessingTimeMs = averageProcessingTimeMs;
                return this;
            }

            public ReconciliationStatistics build {
                return stats;
            }
        }
    }
}