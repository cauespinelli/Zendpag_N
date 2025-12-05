package com.zendapag.core.repository;

import com.zendapag.core.entity.Dispute;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
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
           "d.reasonCode = :reasonCode " +
           "AND d.deleted = false " +
           "ORDER BY d.createdAt DESC")
    Page<Dispute> findByReason(@Param("reasonCode") String reasonCode, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.disputeAmount >= :minAmount " +
           "AND (:maxAmount IS NULL OR d.disputeAmount <= :maxAmount) " +
           "AND d.deleted = false " +
           "ORDER BY d.disputeAmount DESC, d.createdAt DESC")
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
           "d.externalId = :externalId " +
           "AND d.deleted = false")
    Optional<Dispute> findByExternalId(@Param("externalId") String externalId);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.status = :status " +
           "AND d.deleted = false")
    long countByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                 @Param("status") DisputeStatus status);

    @Query("SELECT SUM(d.disputeAmount) FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.status = :status " +
           "AND d.deleted = false")
    BigDecimal sumAmountByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                           @Param("status") DisputeStatus status);

    @Query("SELECT d.status, COUNT(d), SUM(d.disputeAmount) FROM Dispute d WHERE " +
           "d.merchant = :merchant " +
           "AND d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false " +
           "GROUP BY d.status")
    List<Object[]> getDisputeStatsByMerchant(@Param("merchant") Merchant merchant,
                                            @Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate);

    @Query("SELECT d.reasonCode, COUNT(d), SUM(d.disputeAmount) FROM Dispute d WHERE " +
           "d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "AND d.deleted = false " +
           "GROUP BY d.reasonCode " +
           "ORDER BY COUNT(d) DESC")
    List<Object[]> getDisputeReasonStats(@Param("startDate") Instant startDate,
                                        @Param("endDate") Instant endDate);

    @Query("SELECT d FROM Dispute d WHERE " +
           "d.disputeAmount > :threshold " +
           "AND d.deleted = false " +
           "ORDER BY d.disputeAmount DESC, d.createdAt DESC")
    Page<Dispute> findHighValueDisputes(@Param("threshold") BigDecimal threshold, Pageable pageable);

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
}
