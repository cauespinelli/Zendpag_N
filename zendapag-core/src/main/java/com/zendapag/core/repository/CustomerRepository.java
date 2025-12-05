package com.zendapag.core.repository;

import com.zendapag.core.entity.Customer;
import com.zendapag.core.entity.enums.CustomerStatus;
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

    @Query("SELECT c FROM Customer c WHERE c.phone = :phone AND c.deleted = false")
    Optional<Customer> findByPhone(@Param("phone") String phone);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND c.deleted = false")
    List<Customer> findByName(@Param("name") String name);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.status = :status " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByStatus(@Param("status") CustomerStatus status, Pageable pageable);

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
           "c.country = :country " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByCountry(@Param("country") String country, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.state = :state " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByState(@Param("state") String state, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.city = :city " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findByCity(@Param("city") String city, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "c.document LIKE CONCAT('%', :searchTerm, '%') OR " +
           "c.phone LIKE CONCAT('%', :searchTerm, '%') " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> searchCustomers(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE " +
           "c.status = :status " +
           "AND c.deleted = false")
    long countByStatus(@Param("status") CustomerStatus status);

    @Query("SELECT COUNT(c) FROM Customer c WHERE " +
           "c.createdAt >= :startDate AND c.createdAt < :endDate " +
           "AND c.deleted = false")
    long countCreatedBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.riskScore > :threshold " +
           "AND c.deleted = false " +
           "ORDER BY c.riskScore DESC, c.createdAt DESC")
    Page<Customer> findHighRiskCustomers(@Param("threshold") Integer threshold, Pageable pageable);

    @Query("SELECT c.status, COUNT(c) FROM Customer c WHERE " +
           "c.deleted = false " +
           "GROUP BY c.status")
    List<Object[]> getStatusStats();

    @Query("SELECT c.country, COUNT(c) FROM Customer c WHERE " +
           "c.deleted = false " +
           "GROUP BY c.country " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> getCustomerCountryStats();

    @Query("SELECT CAST(c.createdAt AS DATE), COUNT(c) FROM Customer c WHERE " +
           "c.createdAt >= :startDate AND c.createdAt < :endDate " +
           "AND c.deleted = false " +
           "GROUP BY CAST(c.createdAt AS DATE) " +
           "ORDER BY CAST(c.createdAt AS DATE)")
    List<Object[]> getDailyCustomerRegistrations(@Param("startDate") Instant startDate,
                                                 @Param("endDate") Instant endDate);

    @Query("SELECT AVG(c.riskScore) FROM Customer c WHERE " +
           "c.riskScore IS NOT NULL " +
           "AND c.createdAt >= :startDate AND c.createdAt < :endDate " +
           "AND c.deleted = false")
    Double getAverageRiskScore(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE " +
           "c.email = :email AND c.deleted = false")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE " +
           "c.document = :document AND c.deleted = false")
    boolean existsByDocument(@Param("document") String document);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE " +
           "c.phone = :phone AND c.deleted = false")
    boolean existsByPhone(@Param("phone") String phone);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.emailNotifications = true " +
           "AND c.verified = true " +
           "AND c.deleted = false")
    List<Customer> findCustomersEligibleForEmailNotifications();

    @Query("SELECT c FROM Customer c WHERE " +
           "c.smsNotifications = true " +
           "AND c.verified = true " +
           "AND c.deleted = false")
    List<Customer> findCustomersEligibleForSmsNotifications();

    @Query("SELECT COUNT(c) FROM Customer c WHERE " +
           "c.createdAt >= :today " +
           "AND c.deleted = false")
    long countTodayRegistrations(@Param("today") Instant today);
}
