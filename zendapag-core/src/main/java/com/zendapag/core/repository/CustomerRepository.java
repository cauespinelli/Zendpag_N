package com.zendapag.core.repository;

import com.zendapag.core.entity.Customer;
import com.zendapag.core.entity.enums.VerificationStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    @Cacheable(value = "customers", key = "#email")
    Optional<Customer> findByEmail(String email);

    @Cacheable(value = "customers", key = "#document")
    Optional<Customer> findByDocument(String document);

    @Query("SELECT c FROM Customer c WHERE c.phoneNumber = :phoneNumber AND c.deleted = false")
    Optional<Customer> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')) " +
           "AND LOWER(c.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')) " +
           "AND c.deleted = false")
    List<Customer> findByName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.verificationStatus = :status " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByVerificationStatus(@Param("status") VerificationStatus status, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.createdAt >= :startDate AND c.createdAt < :endDate " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByDateRange(@Param("startDate") Instant startDate,
                                   @Param("endDate") Instant endDate,
                                   Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.riskScore >= :minScore " +
           "AND (:maxScore IS NULL OR c.riskScore <= :maxScore) " +
           "AND c.deleted = false " +
           "ORDER BY c.riskScore DESC")
    Page<Customer> findByRiskScoreRange(@Param("minScore") Integer minScore,
                                        @Param("maxScore") Integer maxScore,
                                        Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.address.country = :country " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByCountry(@Param("country") String country, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.address.state = :state " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByState(@Param("state") String state, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.address.city = :city " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByCity(@Param("city") String city, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "c.document LIKE CONCAT('%', :searchTerm, '%') OR " +
           "c.phoneNumber LIKE CONCAT('%', :searchTerm, '%') " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> searchCustomers(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE " +
           "c.verificationStatus = :status " +
           "AND c.deleted = false")
    long countByVerificationStatus(@Param("status") VerificationStatus status);

    @Query("SELECT COUNT(c) FROM Customer c WHERE " +
           "c.createdAt >= :startDate AND c.createdAt < :endDate " +
           "AND c.deleted = false")
    long countCreatedBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.verificationStatus = 'PENDING' " +
           "AND c.createdAt < :before " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt ASC")
    List<Customer> findPendingVerificationCustomersCreatedBefore(@Param("before") Instant before);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.riskScore > :threshold " +
           "AND c.deleted = false " +
           "ORDER BY c.riskScore DESC, c.createdAt DESC")
    Page<Customer> findHighRiskCustomers(@Param("threshold") Integer threshold, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.lastPaymentAt IS NULL OR c.lastPaymentAt < :before " +
           "AND c.deleted = false " +
           "ORDER BY c.lastPaymentAt ASC NULLS FIRST")
    Page<Customer> findInactiveCustomers(@Param("before") Instant before, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.emailVerified = false " +
           "AND c.createdAt < :before " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt ASC")
    List<Customer> findUnverifiedEmailCustomersCreatedBefore(@Param("before") Instant before);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.phoneVerified = false " +
           "AND c.createdAt < :before " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt ASC")
    List<Customer> findUnverifiedPhoneCustomersCreatedBefore(@Param("before") Instant before);

    @Query("SELECT c.verificationStatus, COUNT(c) FROM Customer c WHERE " +
           "c.deleted = false " +
           "GROUP BY c.verificationStatus")
    List<Object[]> getVerificationStatusStats();

    @Query("SELECT c.address.country, COUNT(c) FROM Customer c WHERE " +
           "c.deleted = false " +
           "GROUP BY c.address.country " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> getCustomerCountryStats();

    @Query("SELECT DATE(c.createdAt), COUNT(c) FROM Customer c WHERE " +
           "c.createdAt >= :startDate AND c.createdAt < :endDate " +
           "AND c.deleted = false " +
           "GROUP BY DATE(c.createdAt) " +
           "ORDER BY DATE(c.createdAt)")
    List<Object[]> getDailyCustomerRegistrations(@Param("startDate") Instant startDate,
                                                 @Param("endDate") Instant endDate);

    @Query("SELECT AVG(c.riskScore) FROM Customer c WHERE " +
           "c.riskScore IS NOT NULL " +
           "AND c.createdAt >= :startDate AND c.createdAt < :endDate " +
           "AND c.deleted = false")
    Double getAverageRiskScore(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT " +
           "COUNT(c) as totalCustomers, " +
           "COUNT(CASE WHEN c.verificationStatus = 'VERIFIED' THEN 1 END) as verifiedCustomers, " +
           "COUNT(CASE WHEN c.emailVerified = true THEN 1 END) as emailVerifiedCustomers, " +
           "COUNT(CASE WHEN c.phoneVerified = true THEN 1 END) as phoneVerifiedCustomers, " +
           "AVG(c.riskScore) as averageRiskScore " +
           "FROM Customer c WHERE " +
           "c.createdAt >= :startDate AND c.createdAt < :endDate " +
           "AND c.deleted = false")
    Object getCustomerSummary(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE " +
           "c.email = :email AND c.deleted = false")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE " +
           "c.document = :document AND c.deleted = false")
    boolean existsByDocument(@Param("document") String document);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE " +
           "c.phoneNumber = :phoneNumber AND c.deleted = false")
    boolean existsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.totalPayments > :minPayments " +
           "AND c.totalPaymentAmount > :minAmount " +
           "AND c.deleted = false " +
           "ORDER BY c.totalPaymentAmount DESC, c.totalPayments DESC")
    Page<Customer> findTopCustomers(@Param("minPayments") Integer minPayments,
                                   @Param("minAmount") java.math.BigDecimal minAmount,
                                   Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.preferences.notificationsEnabled = true " +
           "AND c.preferences.emailNotifications = true " +
           "AND c.emailVerified = true " +
           "AND c.deleted = false")
    List<Customer> findCustomersEligibleForEmailNotifications();

    @Query("SELECT c FROM Customer c WHERE " +
           "c.preferences.notificationsEnabled = true " +
           "AND c.preferences.smsNotifications = true " +
           "AND c.phoneVerified = true " +
           "AND c.deleted = false")
    List<Customer> findCustomersEligibleForSmsNotifications();

    @Query("SELECT c FROM Customer c WHERE " +
           "c.tags LIKE CONCAT('%', :tag, '%') " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByTag(@Param("tag") String tag, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE " +
           "c.createdAt >= :today " +
           "AND c.deleted = false")
    long countTodayRegistrations(@Param("today") Instant today);

    @Query(value = "SELECT DATE_TRUNC('hour', c.created_at) as hour, COUNT(*) as count " +
           "FROM customers c " +
           "WHERE c.created_at >= :startDate AND c.created_at < :endDate " +
           "AND c.deleted = false " +
           "GROUP BY DATE_TRUNC('hour', c.created_at) " +
           "ORDER BY hour",
           nativeQuery = true)
    List<Object[]> getHourlyRegistrationVolume(@Param("startDate") Instant startDate,
                                              @Param("endDate") Instant endDate);
}