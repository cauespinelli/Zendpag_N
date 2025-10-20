package com.zendapag.core.repository;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
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
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    @Cacheable(value = "payments", key = "#referenceId")
    Optional<Payment> findByReferenceId(String referenceId);

    @Query("SELECT p FROM Payment p WHERE p.merchant = :merchant AND p.deleted = false ORDER BY p.createdAt DESC")
    Page<Payment> findByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.deleted = false")
    Page<Payment> findByStatus(@Param("status") PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.merchant = :merchant AND p.status = :status " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                          @Param("status") PaymentStatus status,
                                          Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.amount BETWEEN :minAmount AND :maxAmount " +
           "AND p.deleted = false " +
           "ORDER BY p.amount DESC")
    Page<Payment> findByAmountRange(@Param("minAmount") BigDecimal minAmount,
                                    @Param("maxAmount") BigDecimal maxAmount,
                                    Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.createdAt >= :startDate AND p.createdAt < :endDate " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findByDateRange(@Param("startDate") Instant startDate,
                                  @Param("endDate") Instant endDate,
                                  Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.merchant = :merchant " +
           "AND p.createdAt >= :startDate AND p.createdAt < :endDate " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                             @Param("startDate") Instant startDate,
                                             @Param("endDate") Instant endDate,
                                             Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.status IN ('PENDING', 'PROCESSING') " +
           "AND p.expiresAt IS NOT NULL " +
           "AND p.expiresAt < :now " +
           "AND p.deleted = false")
    List<Payment> findExpiredPayments(@Param("now") Instant now);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.status = 'APPROVED' " +
           "AND p.refundableAmount > 0 " +
           "AND p.deleted = false")
    Page<Payment> findRefundablePayments(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Payment p WHERE " +
           "p.merchant = :merchant AND p.status = :status " +
           "AND p.deleted = false")
    long countByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                  @Param("status") PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE " +
           "p.merchant = :merchant AND p.status = 'APPROVED' " +
           "AND p.createdAt >= :startDate AND p.createdAt < :endDate " +
           "AND p.deleted = false")
    BigDecimal sumApprovedAmountByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                                       @Param("startDate") Instant startDate,
                                                       @Param("endDate") Instant endDate);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE " +
           "p.status = 'APPROVED' " +
           "AND p.createdAt >= :startDate AND p.createdAt < :endDate " +
           "AND p.deleted = false")
    BigDecimal sumApprovedAmountByDateRange(@Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.customerEmail = :email " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findByCustomerEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.customerDocument = :document " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findByCustomerDocument(@Param("document") String document, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.gateway = :gateway " +
           "AND p.status = :status " +
           "AND p.deleted = false")
    List<Payment> findByGatewayAndStatus(@Param("gateway") String gateway,
                                         @Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.pixKey = :pixKey " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    List<Payment> findByPixKey(@Param("pixKey") String pixKey);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.pixTransactionId = :pixTransactionId " +
           "AND p.deleted = false")
    Optional<Payment> findByPixTransactionId(@Param("pixTransactionId") String pixTransactionId);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.externalId = :externalId " +
           "AND p.deleted = false")
    Optional<Payment> findByExternalId(@Param("externalId") String externalId);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.gatewayTransactionId = :gatewayTransactionId " +
           "AND p.deleted = false")
    Optional<Payment> findByGatewayTransactionId(@Param("gatewayTransactionId") String gatewayTransactionId);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :status, p.processedAt = :processedAt WHERE p.id = :paymentId")
    int updateStatus(@Param("paymentId") UUID paymentId,
                     @Param("status") PaymentStatus status,
                     @Param("processedAt") Instant processedAt);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.merchant = :merchant " +
           "AND p.createdAt >= :startDate " +
           "AND p.deleted = false " +
           "ORDER BY p.amount DESC")
    Page<Payment> findTopPaymentsByAmount(@Param("merchant") Merchant merchant,
                                          @Param("startDate") Instant startDate,
                                          Pageable pageable);

    @Query("SELECT p.status, COUNT(p), SUM(p.amount) FROM Payment p WHERE " +
           "p.merchant = :merchant " +
           "AND p.createdAt >= :startDate AND p.createdAt < :endDate " +
           "AND p.deleted = false " +
           "GROUP BY p.status")
    List<Object[]> getPaymentStatsByMerchant(@Param("merchant") Merchant merchant,
                                             @Param("startDate") Instant startDate,
                                             @Param("endDate") Instant endDate);

    @Query("SELECT DATE(p.createdAt), COUNT(p), SUM(p.amount) FROM Payment p WHERE " +
           "p.status = 'APPROVED' " +
           "AND p.createdAt >= :startDate AND p.createdAt < :endDate " +
           "AND p.deleted = false " +
           "GROUP BY DATE(p.createdAt) " +
           "ORDER BY DATE(p.createdAt)")
    List<Object[]> getDailyPaymentStats(@Param("startDate") Instant startDate,
                                        @Param("endDate") Instant endDate);

    @Query("SELECT p FROM Payment p WHERE " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "p.referenceId LIKE CONCAT('%', :searchTerm, '%') OR " +
           "p.customerEmail LIKE CONCAT('%', :searchTerm, '%') " +
           "AND p.deleted = false")
    Page<Payment> searchPayments(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT DISTINCT DATE(p.createdAt) FROM Payment p WHERE " +
           "p.merchant = :merchant " +
           "AND p.createdAt >= :startDate AND p.createdAt < :endDate " +
           "AND p.deleted = false " +
           "ORDER BY DATE(p.createdAt) DESC")
    List<LocalDate> getPaymentDates(@Param("merchant") Merchant merchant,
                                    @Param("startDate") Instant startDate,
                                    @Param("endDate") Instant endDate);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.merchant = :merchant " +
           "AND DATE(p.createdAt) = :date " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    List<Payment> findByMerchantAndDate(@Param("merchant") Merchant merchant,
                                        @Param("date") LocalDate date);

    @Query("SELECT AVG(p.amount) FROM Payment p WHERE " +
           "p.status = 'APPROVED' " +
           "AND p.createdAt >= :startDate AND p.createdAt < :endDate " +
           "AND p.deleted = false")
    BigDecimal getAveragePaymentAmount(@Param("startDate") Instant startDate,
                                       @Param("endDate") Instant endDate);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.amount > :threshold " +
           "AND p.status IN ('APPROVED', 'PENDING', 'PROCESSING') " +
           "AND p.deleted = false " +
           "ORDER BY p.amount DESC, p.createdAt DESC")
    Page<Payment> findHighValuePayments(@Param("threshold") BigDecimal threshold, Pageable pageable);

    @Query(value = "SELECT DATE_TRUNC('hour', p.created_at) as hour, COUNT(*) as count " +
           "FROM payments p " +
           "WHERE p.created_at >= :startDate AND p.created_at < :endDate " +
           "AND p.deleted = false " +
           "GROUP BY DATE_TRUNC('hour', p.created_at) " +
           "ORDER BY hour",
           nativeQuery = true)
    List<Object[]> getHourlyPaymentVolume(@Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);

    @Query("SELECT p FROM Payment p WHERE " +
           "p.ipAddress = :ipAddress " +
           "AND p.createdAt >= :since " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    List<Payment> findByIpAddressSince(@Param("ipAddress") String ipAddress,
                                       @Param("since") Instant since);

    @Query("SELECT COUNT(p) FROM Payment p WHERE " +
           "p.merchant = :merchant " +
           "AND p.createdAt >= :today " +
           "AND p.deleted = false")
    long countTodayPaymentsByMerchant(@Param("merchant") Merchant merchant,
                                      @Param("today") Instant today);

    @Query("SELECT " +
           "COUNT(p) as totalPayments, " +
           "COUNT(CASE WHEN p.status = 'APPROVED' THEN 1 END) as approvedPayments, " +
           "COUNT(CASE WHEN p.status = 'REJECTED' THEN 1 END) as rejectedPayments, " +
           "COUNT(CASE WHEN p.status = 'PENDING' THEN 1 END) as pendingPayments, " +
           "SUM(CASE WHEN p.status = 'APPROVED' THEN p.amount ELSE 0 END) as totalApprovedAmount " +
           "FROM Payment p WHERE " +
           "p.merchant = :merchant " +
           "AND p.createdAt >= :startDate AND p.createdAt < :endDate " +
           "AND p.deleted = false")
    Object getPaymentSummaryByMerchant(@Param("merchant") Merchant merchant,
                                       @Param("startDate") Instant startDate,
                                       @Param("endDate") Instant endDate);
}