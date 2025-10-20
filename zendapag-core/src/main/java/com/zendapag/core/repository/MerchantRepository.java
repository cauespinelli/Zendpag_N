package com.zendapag.core.repository;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.enums.MerchantStatus;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID>, JpaSpecificationExecutor<Merchant> {

    @Cacheable(value = "merchants", key = "#document")
    Optional<Merchant> findByDocument(String document);

    @Cacheable(value = "merchants", key = "#email")
    Optional<Merchant> findByEmail(String email);

    @Query("SELECT m FROM Merchant m WHERE m.status = :status AND m.deleted = false")
    List<Merchant> findByStatus(@Param("status") MerchantStatus status);

    @Query("SELECT m FROM Merchant m WHERE m.status = 'ACTIVE' AND m.kycVerified = true AND m.deleted = false")
    List<Merchant> findActiveVerifiedMerchants();

    @Query("SELECT m FROM Merchant m WHERE m.status = 'PENDING_APPROVAL' AND m.createdAt < :before AND m.deleted = false")
    List<Merchant> findPendingMerchantsCreatedBefore(@Param("before") Instant before);

    @Query("SELECT m FROM Merchant m WHERE m.riskScore > :minScore AND m.deleted = false ORDER BY m.riskScore DESC")
    Page<Merchant> findHighRiskMerchants(@Param("minScore") Integer minScore, Pageable pageable);

    @Query("SELECT m FROM Merchant m WHERE " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "m.document LIKE CONCAT('%', :searchTerm, '%') " +
           "AND m.deleted = false")
    Page<Merchant> searchMerchants(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.status = :status AND m.deleted = false")
    long countByStatus(@Param("status") MerchantStatus status);

    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.createdAt >= :startDate AND m.createdAt < :endDate AND m.deleted = false")
    long countCreatedBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT m FROM Merchant m WHERE " +
           "m.feeRate BETWEEN :minFeeRate AND :maxFeeRate " +
           "AND m.deleted = false " +
           "ORDER BY m.feeRate ASC")
    List<Merchant> findByFeeRateRange(@Param("minFeeRate") BigDecimal minFeeRate,
                                      @Param("maxFeeRate") BigDecimal maxFeeRate);

    @Query("SELECT m FROM Merchant m WHERE " +
           "m.transactionLimit IS NOT NULL " +
           "AND m.transactionLimit < :maxLimit " +
           "AND m.status = 'ACTIVE' " +
           "AND m.deleted = false")
    List<Merchant> findWithTransactionLimitBelow(@Param("maxLimit") BigDecimal maxLimit);

    @Query("SELECT m FROM Merchant m WHERE " +
           "m.lastLoginAt IS NULL OR m.lastLoginAt < :before " +
           "AND m.status = 'ACTIVE' " +
           "AND m.deleted = false")
    List<Merchant> findInactiveMerchants(@Param("before") Instant before);

    @Query("SELECT m FROM Merchant m WHERE " +
           "m.webhookUrl IS NOT NULL " +
           "AND m.status = 'ACTIVE' " +
           "AND m.deleted = false")
    List<Merchant> findMerchantsWithWebhooks();

    @Query("SELECT m FROM Merchant m WHERE " +
           "m.autoSettle = true " +
           "AND m.status = 'ACTIVE' " +
           "AND m.deleted = false")
    List<Merchant> findAutoSettleMerchants();

    @Modifying
    @Query("UPDATE Merchant m SET m.lastLoginAt = :loginTime WHERE m.id = :merchantId")
    int updateLastLogin(@Param("merchantId") UUID merchantId, @Param("loginTime") Instant loginTime);

    @Modifying
    @Query("UPDATE Merchant m SET m.riskScore = :riskScore WHERE m.id = :merchantId")
    int updateRiskScore(@Param("merchantId") UUID merchantId, @Param("riskScore") Integer riskScore);

    @Query("SELECT m FROM Merchant m JOIN m.apiKeys ak WHERE ak.keyHash = :keyHash AND ak.status = 'ACTIVE' AND m.deleted = false")
    Optional<Merchant> findByApiKeyHash(@Param("keyHash") String keyHash);

    @Query("SELECT DISTINCT m FROM Merchant m " +
           "LEFT JOIN m.payments p " +
           "WHERE p.createdAt >= :startDate " +
           "AND p.createdAt < :endDate " +
           "AND m.deleted = false " +
           "ORDER BY COUNT(p) DESC")
    List<Merchant> findTopMerchantsByPaymentVolume(@Param("startDate") Instant startDate,
                                                   @Param("endDate") Instant endDate,
                                                   Pageable pageable);

    @Query("SELECT m.status, COUNT(m) FROM Merchant m WHERE m.deleted = false GROUP BY m.status")
    List<Object[]> getMerchantStatusStats();

    @Query(value = "SELECT * FROM merchants m WHERE " +
           "m.created_at >= :startDate AND m.created_at < :endDate " +
           "AND m.deleted = false " +
           "ORDER BY m.created_at DESC",
           nativeQuery = true)
    List<Merchant> findRecentMerchants(@Param("startDate") Instant startDate,
                                       @Param("endDate") Instant endDate);

    @Query("SELECT m FROM Merchant m WHERE " +
           "m.country = :country " +
           "AND m.status = 'ACTIVE' " +
           "AND m.deleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<Merchant> findByCountry(@Param("country") String country, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Merchant m WHERE m.document = :document AND m.deleted = false")
    boolean existsByDocument(@Param("document") String document);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Merchant m WHERE m.email = :email AND m.deleted = false")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT m FROM Merchant m WHERE " +
           "m.kycVerified = false " +
           "AND m.status IN ('PENDING_APPROVAL', 'ACTIVE') " +
           "AND m.deleted = false " +
           "ORDER BY m.createdAt ASC")
    List<Merchant> findMerchantsRequiringKycVerification();

    // Custom method for merchant statistics
    @Query("SELECT " +
           "COUNT(m) as totalMerchants, " +
           "COUNT(CASE WHEN m.status = 'ACTIVE' THEN 1 END) as activeMerchants, " +
           "COUNT(CASE WHEN m.kycVerified = true THEN 1 END) as verifiedMerchants, " +
           "AVG(m.riskScore) as averageRiskScore " +
           "FROM Merchant m WHERE m.deleted = false")
    Object getMerchantStatistics();
}