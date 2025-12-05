package com.zendapag.core.repository;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Settlement;
import com.zendapag.core.entity.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID>, JpaSpecificationExecutor<Settlement> {

    @Query("SELECT s FROM Settlement s WHERE s.merchant = :merchant AND s.deleted = false ORDER BY s.createdAt DESC")
    Page<Settlement> findByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.status = :status " +
           "AND s.deleted = false " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findByStatus(@Param("status") SettlementStatus status, Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.status = :status " +
           "AND s.deleted = false " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                            @Param("status") SettlementStatus status,
                                            Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.settlementDate = :settlementDate " +
           "AND s.deleted = false " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findBySettlementDate(@Param("settlementDate") LocalDate settlementDate, Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.settlementDate = :settlementDate " +
           "AND s.deleted = false " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findByMerchantAndSettlementDate(@Param("merchant") Merchant merchant,
                                                    @Param("settlementDate") LocalDate settlementDate,
                                                    Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.settlementDate >= :startDate AND s.settlementDate <= :endDate " +
           "AND s.deleted = false " +
           "ORDER BY s.settlementDate DESC, s.createdAt DESC")
    Page<Settlement> findBySettlementDateRange(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.settlementDate >= :startDate AND s.settlementDate <= :endDate " +
           "AND s.deleted = false " +
           "ORDER BY s.settlementDate DESC, s.createdAt DESC")
    Page<Settlement> findByMerchantAndSettlementDateRange(@Param("merchant") Merchant merchant,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.settlementDate >= :startDate AND s.settlementDate <= :endDate " +
           "AND s.deleted = false " +
           "ORDER BY s.settlementDate DESC, s.createdAt DESC")
    List<Settlement> findByMerchantAndSettlementDateBetween(@Param("merchant") Merchant merchant,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.createdAt >= :startDate AND s.createdAt < :endDate " +
           "AND s.deleted = false " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findByCreatedDateRange(@Param("startDate") Instant startDate,
                                           @Param("endDate") Instant endDate,
                                           Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.grossAmount >= :minAmount " +
           "AND (:maxAmount IS NULL OR s.grossAmount <= :maxAmount) " +
           "AND s.deleted = false " +
           "ORDER BY s.grossAmount DESC, s.createdAt DESC")
    Page<Settlement> findByGrossAmountRange(@Param("minAmount") BigDecimal minAmount,
                                           @Param("maxAmount") BigDecimal maxAmount,
                                           Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.netAmount >= :minAmount " +
           "AND (:maxAmount IS NULL OR s.netAmount <= :maxAmount) " +
           "AND s.deleted = false " +
           "ORDER BY s.netAmount DESC, s.createdAt DESC")
    Page<Settlement> findByNetAmountRange(@Param("minAmount") BigDecimal minAmount,
                                         @Param("maxAmount") BigDecimal maxAmount,
                                         Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.status = 'PENDING' " +
           "AND s.settlementDate <= :today " +
           "AND s.deleted = false " +
           "ORDER BY s.settlementDate ASC, s.createdAt ASC")
    List<Settlement> findPendingSettlementsReadyForProcessing(@Param("today") LocalDate today);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.status = 'PROCESSING' " +
           "AND s.processedAt < :before " +
           "AND s.deleted = false " +
           "ORDER BY s.processedAt ASC")
    List<Settlement> findStuckProcessingSettlements(@Param("before") Instant before);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.externalId = :externalId " +
           "AND s.deleted = false")
    Optional<Settlement> findByExternalId(@Param("externalId") String externalId);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.bankTransactionId = :reference " +
           "AND s.deleted = false")
    Optional<Settlement> findByBankTransactionId(@Param("reference") String reference);

    @Query("SELECT COUNT(s) FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.status = :status " +
           "AND s.deleted = false")
    long countByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                 @Param("status") SettlementStatus status);

    @Query("SELECT SUM(s.netAmount) FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.status = 'COMPLETED' " +
           "AND s.settlementDate >= :startDate AND s.settlementDate <= :endDate " +
           "AND s.deleted = false")
    BigDecimal sumCompletedNetAmountByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(s.grossAmount) FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.status = 'COMPLETED' " +
           "AND s.settlementDate >= :startDate AND s.settlementDate <= :endDate " +
           "AND s.deleted = false")
    BigDecimal sumCompletedGrossAmountByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(s.feeAmount) FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.status = 'COMPLETED' " +
           "AND s.settlementDate >= :startDate AND s.settlementDate <= :endDate " +
           "AND s.deleted = false")
    BigDecimal sumFeeAmountByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT s.status, COUNT(s), SUM(s.grossAmount), SUM(s.netAmount) FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.createdAt >= :startDate AND s.createdAt < :endDate " +
           "AND s.deleted = false " +
           "GROUP BY s.status")
    List<Object[]> getSettlementStatsByMerchant(@Param("merchant") Merchant merchant,
                                               @Param("startDate") Instant startDate,
                                               @Param("endDate") Instant endDate);

    // Temporarily commented out - JPQL GROUP BY LocalDate issue with H2
    // @Query("SELECT s.settlementDate, COUNT(s), SUM(s.grossAmount), SUM(s.netAmount) FROM Settlement s WHERE " +
    //        "s.settlementDate >= :startDate AND s.settlementDate <= :endDate " +
    //        "AND s.status = 'COMPLETED' " +
    //        "AND s.deleted = false " +
    //        "GROUP BY s.settlementDate " +
    //        "ORDER BY s.settlementDate")
    // List<Object[]> getDailySettlementStats(@Param("startDate") LocalDate startDate,
    //                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.description LIKE CONCAT('%', :searchTerm, '%') OR " +
           "s.externalId LIKE CONCAT('%', :searchTerm, '%') OR " +
           "s.bankTransactionId LIKE CONCAT('%', :searchTerm, '%') " +
           "AND s.deleted = false " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> searchSettlements(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT AVG(s.transactionCount) FROM Settlement s WHERE " +
           "s.status = 'COMPLETED' " +
           "AND s.createdAt >= :startDate AND s.createdAt < :endDate " +
           "AND s.deleted = false")
    Double getAverageTransactionCount(@Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate);

    @Query("SELECT AVG(s.netAmount) FROM Settlement s WHERE " +
           "s.status = 'COMPLETED' " +
           "AND s.createdAt >= :startDate AND s.createdAt < :endDate " +
           "AND s.deleted = false")
    BigDecimal getAverageSettlementAmount(@Param("startDate") Instant startDate,
                                         @Param("endDate") Instant endDate);

    @Query("SELECT " +
           "COUNT(s) as totalSettlements, " +
           "COUNT(CASE WHEN s.status = 'COMPLETED' THEN 1 END) as completedSettlements, " +
           "COUNT(CASE WHEN s.status = 'PENDING' THEN 1 END) as pendingSettlements, " +
           "COUNT(CASE WHEN s.status = 'FAILED' THEN 1 END) as failedSettlements, " +
           "SUM(s.grossAmount) as totalGrossAmount, " +
           "SUM(s.netAmount) as totalNetAmount, " +
           "SUM(s.feeAmount) as totalFeeAmount " +
           "FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.createdAt >= :startDate AND s.createdAt < :endDate " +
           "AND s.deleted = false")
    Object getSettlementSummaryByMerchant(@Param("merchant") Merchant merchant,
                                         @Param("startDate") Instant startDate,
                                         @Param("endDate") Instant endDate);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.status = 'COMPLETED' " +
           "AND s.deleted = false " +
           "ORDER BY s.netAmount DESC")
    Page<Settlement> findTopSettlementsByAmount(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Settlement s WHERE " +
           "s.createdAt >= :today " +
           "AND s.deleted = false")
    long countTodaySettlements(@Param("today") Instant today);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.settlementDate = :tomorrow " +
           "AND s.status = 'PENDING' " +
           "AND s.deleted = false " +
           "ORDER BY s.createdAt ASC")
    List<Settlement> findSettlementsScheduledForTomorrow(@Param("tomorrow") LocalDate tomorrow);

    // Temporarily commented out - H2 does not support DATETRUNC function
    // @Query(value = "SELECT DATETRUNC('hour', s.created_at) as hour, " +
    // "s.status, COUNT(*) as count, SUM(s.net_amount) as total " +
    // "FROM settlements s " +
    // "WHERE s.created_at >= :startDate AND s.created_at < :endDate " +
    // "AND s.deleted = false " +
    // "GROUP BY DATETRUNC('hour', s.created_at), s.status " +
    // "ORDER BY hour, s.status",
    // nativeQuery = true)
    // List<Object[]> getHourlySettlementVolume(@Param("startDate") Instant startDate,
    // @Param("endDate") Instant endDate);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.merchant = :merchant " +
           "AND s.status IN ('FAILED', 'CANCELLED') " +
           "AND s.createdAt >= :since " +
           "AND s.deleted = false " +
           "ORDER BY s.createdAt DESC")
    List<Settlement> findRecentFailuresForMerchant(@Param("merchant") Merchant merchant,
                                                  @Param("since") Instant since);

    // H2 compatible: using DAY_OF_WEEK function instead of EXTRACT(DOW FROM ...)
    @Query(value = "SELECT DAY_OF_WEEK(s.settlement_date) as dayOfWeek, " +
           "COUNT(*) as count, " +
           "SUM(s.net_amount) as totalAmount " +
           "FROM settlements s WHERE " +
           "s.status = 'COMPLETED' " +
           "AND s.settlement_date >= :startDate AND s.settlement_date <= :endDate " +
           "AND s.deleted = false " +
           "GROUP BY DAY_OF_WEEK(s.settlement_date) " +
           "ORDER BY dayOfWeek",
           nativeQuery = true)
    List<Object[]> getSettlementsByDayOfWeek(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.netAmount > :threshold " +
           "AND s.deleted = false " +
           "ORDER BY s.netAmount DESC, s.createdAt DESC")
    Page<Settlement> findHighValueSettlements(@Param("threshold") BigDecimal threshold, Pageable pageable);

    // Note: findByTag removed - Settlement entity does not have a tags field

    @Query("SELECT s FROM Settlement s WHERE " +
           "s.currency = :currency " +
           "AND s.deleted = false " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findByCurrency(@Param("currency") String currency, Pageable pageable);

    // H2 compatible: using DATEDIFF instead of EXTRACT(EPOCH FROM ...)
    @Query(value = "SELECT AVG(DATEDIFF(SECOND, s.created_at, s.processed_at) / 3600.0) FROM settlements s WHERE " +
           "s.status = 'COMPLETED' " +
           "AND s.processed_at IS NOT NULL " +
           "AND s.created_at >= :startDate AND s.created_at < :endDate " +
           "AND s.deleted = false",
           nativeQuery = true)
    Double getAverageProcessingTimeInHours(@Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);
}