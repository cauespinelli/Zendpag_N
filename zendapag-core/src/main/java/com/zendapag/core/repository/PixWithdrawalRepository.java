package com.zendapag.core.repository;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.entity.enums.WithdrawalStatus;
import org.springframework.cache.annotation.Cacheable;
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

/**
 * Repository para PixWithdrawal
 */
@Repository
public interface PixWithdrawalRepository extends JpaRepository<PixWithdrawal, UUID>, JpaSpecificationExecutor<PixWithdrawal> {

    @Cacheable(value = "withdrawals", key = "#referenceId")
    Optional<PixWithdrawal> findByReferenceId(String referenceId);

    @Query("SELECT w FROM PixWithdrawal w WHERE w.account = :account AND w.deleted = false ORDER BY w.createdAt DESC")
    Page<PixWithdrawal> findByAccount(@Param("account") Account account, Pageable pageable);

    @Query("SELECT w FROM PixWithdrawal w WHERE w.merchant = :merchant AND w.deleted = false ORDER BY w.createdAt DESC")
    Page<PixWithdrawal> findByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.merchant = :merchant AND w.status = :status " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Page<PixWithdrawal> findByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                                 @Param("status") WithdrawalStatus status,
                                                 Pageable pageable);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.account = :account AND w.status = :status " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Page<PixWithdrawal> findByAccountAndStatus(@Param("account") Account account,
                                                @Param("status") WithdrawalStatus status,
                                                Pageable pageable);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.status = :status " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt ASC")
    List<PixWithdrawal> findByStatus(@Param("status") WithdrawalStatus status);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.merchant = :merchant " +
           "AND w.requestedAt >= :startDate AND w.requestedAt < :endDate " +
           "AND w.deleted = false " +
           "ORDER BY w.requestedAt DESC")
    Page<PixWithdrawal> findByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                                    @Param("startDate") Instant startDate,
                                                    @Param("endDate") Instant endDate,
                                                    Pageable pageable);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.account = :account " +
           "AND w.requestedAt >= :startDate AND w.requestedAt < :endDate " +
           "AND w.deleted = false " +
           "ORDER BY w.requestedAt DESC")
    Page<PixWithdrawal> findByAccountAndDateRange(@Param("account") Account account,
                                                   @Param("startDate") Instant startDate,
                                                   @Param("endDate") Instant endDate,
                                                   Pageable pageable);

    @Query("SELECT SUM(w.amount) FROM PixWithdrawal w WHERE " +
           "w.account = :account " +
           "AND w.status IN ('PENDING', 'PROCESSING', 'APPROVED') " +
           "AND w.deleted = false")
    BigDecimal sumPendingWithdrawalsByAccount(@Param("account") Account account);

    @Query("SELECT SUM(w.amount) FROM PixWithdrawal w WHERE " +
           "w.account = :account " +
           "AND w.status = 'COMPLETED' " +
           "AND w.completedAt >= :startDate AND w.completedAt < :endDate " +
           "AND w.deleted = false")
    BigDecimal sumCompletedWithdrawalsByAccountAndDateRange(@Param("account") Account account,
                                                            @Param("startDate") Instant startDate,
                                                            @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(w) FROM PixWithdrawal w WHERE " +
           "w.account = :account " +
           "AND w.requestedAt >= :since " +
           "AND w.deleted = false")
    long countWithdrawalsSince(@Param("account") Account account,
                               @Param("since") Instant since);

    @Query("SELECT COUNT(w) FROM PixWithdrawal w WHERE " +
           "w.account = :account " +
           "AND w.status IN ('PENDING', 'PROCESSING', 'APPROVED') " +
           "AND w.deleted = false")
    long countPendingWithdrawalsByAccount(@Param("account") Account account);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.status = 'PENDING' " +
           "AND w.expiresAt IS NOT NULL " +
           "AND w.expiresAt < :now " +
           "AND w.deleted = false")
    List<PixWithdrawal> findExpiredPendingWithdrawals(@Param("now") Instant now);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.pixEndToEndId = :endToEndId " +
           "AND w.deleted = false")
    Optional<PixWithdrawal> findByPixEndToEndId(@Param("endToEndId") String endToEndId);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.pixTransactionId = :transactionId " +
           "AND w.deleted = false")
    Optional<PixWithdrawal> findByPixTransactionId(@Param("transactionId") String transactionId);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.externalReference = :externalReference " +
           "AND w.merchant = :merchant " +
           "AND w.deleted = false")
    Optional<PixWithdrawal> findByMerchantAndExternalReference(@Param("merchant") Merchant merchant,
                                                               @Param("externalReference") String externalReference);

    @Query("SELECT w.status, COUNT(w), SUM(w.amount) FROM PixWithdrawal w WHERE " +
           "w.merchant = :merchant " +
           "AND w.requestedAt >= :startDate AND w.requestedAt < :endDate " +
           "AND w.deleted = false " +
           "GROUP BY w.status")
    List<Object[]> getWithdrawalStatsByMerchant(@Param("merchant") Merchant merchant,
                                                @Param("startDate") Instant startDate,
                                                @Param("endDate") Instant endDate);

    @Query("SELECT w.status, COUNT(w), SUM(w.amount) FROM PixWithdrawal w WHERE " +
           "w.account = :account " +
           "AND w.requestedAt >= :startDate AND w.requestedAt < :endDate " +
           "AND w.deleted = false " +
           "GROUP BY w.status")
    List<Object[]> getWithdrawalStatsByAccount(@Param("account") Account account,
                                               @Param("startDate") Instant startDate,
                                               @Param("endDate") Instant endDate);

    @Query("SELECT AVG(EXTRACT(EPOCH FROM (w.completedAt - w.requestedAt))) FROM PixWithdrawal w WHERE " +
           "w.status = 'COMPLETED' " +
           "AND w.completedAt IS NOT NULL " +
           "AND w.requestedAt >= :startDate AND w.requestedAt < :endDate " +
           "AND w.deleted = false")
    Double getAverageProcessingTimeInSeconds(@Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.pixKey = :pixKey " +
           "AND w.deleted = false " +
           "ORDER BY w.requestedAt DESC")
    Page<PixWithdrawal> findByPixKey(@Param("pixKey") String pixKey, Pageable pageable);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.status IN ('PENDING', 'PROCESSING') " +
           "AND w.requestedAt < :before " +
           "AND w.deleted = false " +
           "ORDER BY w.requestedAt ASC")
    List<PixWithdrawal> findStuckWithdrawals(@Param("before") Instant before);

    @Query("SELECT SUM(w.feeAmount) FROM PixWithdrawal w WHERE " +
           "w.merchant = :merchant " +
           "AND w.status = 'COMPLETED' " +
           "AND w.completedAt >= :startDate AND w.completedAt < :endDate " +
           "AND w.deleted = false")
    BigDecimal sumFeesByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                            @Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(w) FROM PixWithdrawal w WHERE " +
           "w.account = :account " +
           "AND w.requestedAt >= :today " +
           "AND w.deleted = false")
    long countTodayWithdrawalsByAccount(@Param("account") Account account,
                                       @Param("today") Instant today);

    @Query("SELECT SUM(w.amount) FROM PixWithdrawal w WHERE " +
           "w.account = :account " +
           "AND w.requestedAt >= :today " +
           "AND w.deleted = false")
    BigDecimal sumTodayWithdrawalAmountByAccount(@Param("account") Account account,
                                                 @Param("today") Instant today);

    @Query("SELECT w FROM PixWithdrawal w WHERE " +
           "w.amount > :threshold " +
           "AND w.status = 'PENDING' " +
           "AND w.deleted = false " +
           "ORDER BY w.amount DESC, w.requestedAt ASC")
    List<PixWithdrawal> findHighValuePendingWithdrawals(@Param("threshold") BigDecimal threshold);
}
