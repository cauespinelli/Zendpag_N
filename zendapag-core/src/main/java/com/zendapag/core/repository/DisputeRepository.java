package com.zendapag.core.repository;

import com.zendapag.core.entity.Dispute;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.DisputeReason;
import com.zendapag.core.entity.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID>, JpaSpecificationExecutor<Dispute> {

    @Query("SELECT d FROM Dispute d WHERE d.merchant = :merchant AND d.deleted = false ORDER BY d.createdAt DESC")
    Page<Dispute> findByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE d.payment = :payment AND d.deleted = false ORDER BY d.createdAt DESC")
    List<Dispute> findByPayment(@Param("payment") Payment payment);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.status = :status " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> findByStatus(@Param("status") DisputeStatus status, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.status = :status " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> findByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                         @Param("status") DisputeStatus status,
                                         Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.reason = :reason " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> findByReason(@Param("reason") DisputeReason reason, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.reason = :reason " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> findByMerchantAndReason(@Param("merchant") Merchant merchant,
                                         @Param("reason") DisputeReason reason,
                                         Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.amount >= :minAmount " +
           "AND (:maxAmount IS NULL OR d.amount <= :maxAmount) " +
           "AND d.deleted = false " +
           "ORDER BY d.amount DESC, d.createdAt DESC")
    Page<Dispute> findByAmountRange(@Param("minAmount") BigDecimal minAmount,
                                   @Param("maxAmount") BigDecimal maxAmount,
                                   Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> findByDateRange(@Param("startDate") Instant startDate,
                                 @Param("endDate") Instant endDate,
                                 Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> findByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                            @Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate,
                                            Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.responseDeadline <= :deadline " +
           "AND d.status IN ('OPEN', 'UNDER_REVIEW') " +
           "AND d.deleted = false " +
           "ORDER BY d.responseDeadline ASC")
    List<Dispute> findDisputesApproachingDeadline(@Param("deadline") Instant deadline);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.responseDeadline < :now " +
           "AND d.status IN ('OPEN', 'UNDER_REVIEW') " +
           "AND d.deleted = false " +
           "ORDER BY d.responseDeadline ASC")
    List<Dispute> findOverdueDisputes(@Param("now") Instant now);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.externalId = :externalId " +
           "AND d.deleted = false")
    Optional<Dispute> findByExternalId(@Param("externalId") String externalId);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.status = :status " +
           "AND d.deleted = false")
    long countByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                 @Param("status") DisputeStatus status);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE " +
           "d.reason = :reason " +
           "AND d.status = :status " +
           "AND d.deleted = false")
    long countByReasonAndStatus(@Param("reason") DisputeReason reason,
                               @Param("status") DisputeStatus status);

    @Query("SELECT SUM(d.amount) FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.status = :status " +
           "AND d.deleted = false")
    BigDecimal sumAmountByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                           @Param("status") DisputeStatus status);

    @Query("SELECT SUM(d.amount) FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false")
    BigDecimal sumAmountByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                              @Param("startDate") Instant startDate,
                                              @Param("endDate") Instant endDate);

    @Query("SELECT d.status, COUNT(d), SUM(d.amount) FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false " +
           "GROUP BY d.status")
    List<Object[]> getDisputeStatsByMerchant(@Param("merchant") Merchant merchant,
                                            @Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate);

    @Query("SELECT d.reason, COUNT(d), SUM(d.amount) FROM Dispute d WHERE " +
           "d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false " +
           "GROUP BY d.reason " +
           "ORDER BY COUNT(d) DESC")
    List<Object[]> getDisputeReasonStats(@Param("startDate") Instant startDate,
                                        @Param("endDate") Instant endDate);

    @Query("SELECT DATE(d.createdAt), d.status, COUNT(d), SUM(d.amount) FROM Dispute d WHERE " +
           "d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false " +
           "GROUP BY DATE(d.createdAt), d.status " +
           "ORDER BY DATE(d.createdAt), d.status")
    List<Object[]> getDailyDisputeStats(@Param("startDate") Instant startDate,
                                       @Param("endDate") Instant endDate);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.amount > :threshold " +
           "AND d.deleted = false " +
           "ORDER BY d.amount DESC, d.createdAt DESC")
    Page<Dispute> findHighValueDisputes(@Param("threshold") BigDecimal threshold, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.description LIKE CONCAT('%', :searchTerm, '%') OR " +
           "d.externalId LIKE CONCAT('%', :searchTerm, '%') OR " +
           "d.customerName LIKE CONCAT('%', :searchTerm, '%') " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> searchDisputes(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT AVG(EXTRACT(EPOCH FROM (d.resolvedAt - d.createdAt)) / 86400) FROM Dispute d WHERE " +
           "d.status IN ('WON', 'LOST', 'ACCEPTED') " +
           "AND d.resolvedAt IS NOT NULL " +
           "AND d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false")
    Double getAverageResolutionTimeInDays(@Param("startDate") Instant startDate,
                                         @Param("endDate") Instant endDate);

    @Query("SELECT " +
           "COUNT(d) as totalDisputes, " +
           "COUNT(CASE WHEN d.status = 'WON' THEN 1 END) as wonDisputes, " +
           "COUNT(CASE WHEN d.status = 'LOST' THEN 1 END) as lostDisputes, " +
           "COUNT(CASE WHEN d.status = 'OPEN' THEN 1 END) as openDisputes, " +
           "SUM(d.amount) as totalAmount, " +
           "SUM(CASE WHEN d.status = 'WON' THEN d.amount ELSE 0 END) as wonAmount, " +
           "SUM(CASE WHEN d.status = 'LOST' THEN d.amount ELSE 0 END) as lostAmount " +
           "FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false")
    Object getDisputeSummaryByMerchant(@Param("merchant") Merchant merchant,
                                      @Param("startDate") Instant startDate,
                                      @Param("endDate") Instant endDate);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.status IN ('WON', 'LOST') " +
           "AND d.resolvedAt IS NOT NULL " +
           "AND d.deleted = false " +
           "ORDER BY d.resolvedAt DESC")
    Page<Dispute> findResolvedDisputesByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE " +
           "d.createdAt >= :today " +
           "AND d.deleted = false")
    long countTodayDisputes(@Param("today") Instant today);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.status IN ('OPEN', 'UNDER_REVIEW') " +
           "AND d.responseDeadline BETWEEN :startDate AND :endDate " +
           "AND d.deleted = false " +
           "ORDER BY d.responseDeadline ASC")
    List<Dispute> findActiveDisputesWithDeadlineInRange(@Param("merchant") Merchant merchant,
                                                       @Param("startDate") Instant startDate,
                                                       @Param("endDate") Instant endDate);

    @Query(value = "SELECT DATE_TRUNC('hour', d.created_at) as hour, " +
           "d.status, COUNT(*) as count, SUM(d.amount) as total " +
           "FROM disputes d " +
           "WHERE d.created_at >= :startDate AND d.created_at < :endDate " +
           "AND d.deleted = false " +
           "GROUP BY DATE_TRUNC('hour', d.created_at), d.status " +
           "ORDER BY hour, d.status",
           nativeQuery = true)
    List<Object[]> getHourlyDisputeVolume(@Param("startDate") Instant startDate,
                                         @Param("endDate") Instant endDate);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.evidenceSubmittedAt IS NULL " +
           "AND d.status IN ('OPEN', 'UNDER_REVIEW') " +
           "AND d.deleted = false " +
           "ORDER BY d.responseDeadline ASC")
    List<Dispute> findDisputesWithoutEvidence(@Param("merchant") Merchant merchant);

    @Query("SELECT " +
           "CASE WHEN COUNT(CASE WHEN d.status IN ('WON', 'LOST') THEN 1 END) = 0 THEN 0 " +
           "ELSE CAST(COUNT(CASE WHEN d.status = 'WON' THEN 1 END) AS DOUBLE) / " +
           "COUNT(CASE WHEN d.status IN ('WON', 'LOST') THEN 1 END) * 100 END " +
           "FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false")
    Double getWinRateByMerchant(@Param("merchant") Merchant merchant,
                               @Param("startDate") Instant startDate,
                               @Param("endDate") Instant endDate);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.tags LIKE CONCAT('%', :tag, '%') " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> findByTag(@Param("tag") String tag, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.liability = :liability " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> findByLiability(@Param("liability") String liability, Pageable pageable);
}