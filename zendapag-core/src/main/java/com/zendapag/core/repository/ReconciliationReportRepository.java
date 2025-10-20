package com.zendapag.core.repository;

import com.zendapag.core.entity.ReconciliationReport;
import com.zendapag.core.entity.ReconciliationReport.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ReconciliationReport entities
 */
@Repository
public interface ReconciliationReportRepository extends JpaRepository<ReconciliationReport, UUID> {

    /**
     * Find reconciliation report by date
     */
    Optional<ReconciliationReport> findByReconciliationDate(LocalDate date);

    /**
     * Find reports within date range
     */
    List<ReconciliationReport> findByReconciliationDateBetweenOrderByReconciliationDateDesc(
        LocalDate startDate, LocalDate endDate);

    /**
     * Find reports by status
     */
    List<ReconciliationReport> findByStatusOrderByReconciliationDateDesc(ReconciliationStatus status);

    /**
     * Find reports with discrepancies
     */
    @Query("SELECT r FROM ReconciliationReport r WHERE r.discrepancyCount > 0 ORDER BY r.reconciliationDate DESC")
    List<ReconciliationReport> findReportsWithDiscrepancies();

    /**
     * Find reports with discrepancies in date range
     */
    @Query("SELECT r FROM ReconciliationReport r WHERE r.discrepancyCount > 0 AND r.reconciliationDate BETWEEN :startDate AND :endDate ORDER BY r.reconciliationDate DESC")
    List<ReconciliationReport> findReportsWithDiscrepanciesBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    /**
     * Find reports requiring manual review
     */
    List<ReconciliationReport> findByStatusInOrderByReconciliationDateDesc(List<ReconciliationStatus> statuses);

    /**
     * Find recent reports (paginated)
     */
    Page<ReconciliationReport> findByOrderByReconciliationDateDesc(Pageable pageable);

    /**
     * Find reports by date range and status
     */
    @Query("SELECT r FROM ReconciliationReport r WHERE r.reconciliationDate BETWEEN :startDate AND :endDate AND r.status = :status ORDER BY r.reconciliationDate DESC")
    List<ReconciliationReport> findByDateRangeAndStatus(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") ReconciliationStatus status);

    /**
     * Get reconciliation statistics for date range
     */
    @Query("""
        SELECT new map(
            COUNT(r) as totalReports,
            SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedReports,
            SUM(CASE WHEN r.status = 'COMPLETED_WITH_DISCREPANCIES' THEN 1 ELSE 0 END) as reportsWithDiscrepancies,
            SUM(CASE WHEN r.status = 'FAILED' THEN 1 ELSE 0 END) as failedReports,
            SUM(r.internalTransactionCount) as totalInternalTransactions,
            SUM(r.externalTransactionCount) as totalExternalTransactions,
            SUM(r.matchedTransactionCount) as totalMatchedTransactions,
            SUM(r.discrepancyCount) as totalDiscrepancies
        )
        FROM ReconciliationReport r
        WHERE r.reconciliationDate BETWEEN :startDate AND :endDate
        """)
    Object getReconciliationStatistics(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    /**
     * Find reports with processing time above threshold
     */
    @Query("SELECT r FROM ReconciliationReport r WHERE r.processingDurationMs > :thresholdMs ORDER BY r.processingDurationMs DESC")
    List<ReconciliationReport> findSlowReconciliations(@Param("thresholdMs") Long thresholdMs);

    /**
     * Get average processing time for date range
     */
    @Query("SELECT AVG(r.processingDurationMs) FROM ReconciliationReport r WHERE r.reconciliationDate BETWEEN :startDate AND :endDate AND r.processingDurationMs IS NOT NULL")
    Double getAverageProcessingTime(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    /**
     * Find reports created after specific date
     */
    List<ReconciliationReport> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAfter);

    /**
     * Check if reconciliation exists for date
     */
    boolean existsByReconciliationDate(LocalDate date);

    /**
     * Get latest reconciliation report
     */
    Optional<ReconciliationReport> findTopByOrderByReconciliationDateDesc();

    /**
     * Find reports with amount differences above threshold
     */
    @Query("SELECT r FROM ReconciliationReport r WHERE ABS(r.amountDifference) > :threshold ORDER BY ABS(r.amountDifference) DESC")
    List<ReconciliationReport> findReportsWithSignificantAmountDifferences(@Param("threshold") java.math.BigDecimal threshold);

    /**
     * Get success rate for date range
     */
    @Query("""
        SELECT
            CAST(SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END) AS double) / COUNT(r) * 100
        FROM ReconciliationReport r
        WHERE r.reconciliationDate BETWEEN :startDate AND :endDate
        """)
    Double getSuccessRatePercentage(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    /**
     * Find oldest unresolved report
     */
    @Query("SELECT r FROM ReconciliationReport r WHERE r.status IN ('COMPLETED_WITH_DISCREPANCIES', 'REQUIRES_MANUAL_REVIEW') ORDER BY r.reconciliationDate ASC")
    List<ReconciliationReport> findOldestUnresolvedReports(Pageable pageable);

    /**
     * Count reports by status for date range
     */
    @Query("SELECT r.status, COUNT(r) FROM ReconciliationReport r WHERE r.reconciliationDate BETWEEN :startDate AND :endDate GROUP BY r.status")
    List<Object[]> countByStatusInDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * Find reports with high discrepancy count
     */
    @Query("SELECT r FROM ReconciliationReport r WHERE r.discrepancyCount >= :minDiscrepancies ORDER BY r.discrepancyCount DESC")
    List<ReconciliationReport> findReportsWithHighDiscrepancyCount(@Param("minDiscrepancies") Integer minDiscrepancies);

    /**
     * Get monthly reconciliation summary
     */
    @Query("""
        SELECT new map(
            FUNCTION('YEAR', r.reconciliationDate) as year,
            FUNCTION('MONTH', r.reconciliationDate) as month,
            COUNT(r) as reportCount,
            SUM(r.discrepancyCount) as totalDiscrepancies,
            AVG(r.processingDurationMs) as avgProcessingTime
        )
        FROM ReconciliationReport r
        WHERE r.reconciliationDate BETWEEN :startDate AND :endDate
        GROUP BY FUNCTION('YEAR', r.reconciliationDate), FUNCTION('MONTH', r.reconciliationDate)
        ORDER BY year DESC, month DESC
        """)
    List<Object> getMonthlyReconciliationSummary(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
}