package com.zendapag.core.repository;

import com.zendapag.core.entity.Customer;
import com.zendapag.core.entity.PaymentMethod;
import com.zendapag.core.entity.enums.PaymentMethodStatus;
import com.zendapag.core.entity.enums.PaymentMethodType;
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
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID>, JpaSpecificationExecutor<PaymentMethod> {

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.customer = :customer AND pm.deleted = false ORDER BY pm.isDefault DESC, pm.createdAt DESC")
    Page<PaymentMethod> findByCustomer(@Param("customer") Customer customer, Pageable pageable);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.customer = :customer AND pm.deleted = false ORDER BY pm.isDefault DESC, pm.createdAt DESC")
    List<PaymentMethod> findByCustomer(@Param("customer") Customer customer);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.customer = :customer " +
           "AND pm.type = :type " +
           "AND pm.deleted = false " +
           "ORDER BY pm.isDefault DESC, pm.createdAt DESC")
    List<PaymentMethod> findByCustomerAndType(@Param("customer") Customer customer,
                                             @Param("type") PaymentMethodType type);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.customer = :customer " +
           "AND pm.isDefault = true " +
           "AND pm.status = 'ACTIVE' " +
           "AND pm.deleted = false")
    Optional<PaymentMethod> findDefaultByCustomer(@Param("customer") Customer customer);

    @Cacheable(value = "paymentMethods", key = "#tokenId")
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.tokenId = :tokenId AND pm.deleted = false")
    Optional<PaymentMethod> findByTokenId(@Param("tokenId") String tokenId);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.type = :type " +
           "AND pm.status = :status " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findByTypeAndStatus(@Param("type") PaymentMethodType type,
                                           @Param("status") PaymentMethodStatus status,
                                           Pageable pageable);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.status = :status " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findByStatus(@Param("status") PaymentMethodStatus status, Pageable pageable);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.cardDetails.lastFourDigits = :lastFour " +
           "AND pm.type = 'CREDIT_CARD' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    List<PaymentMethod> findCreditCardsByLastFour(@Param("lastFour") String lastFour);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.pixDetails.pixKey = :pixKey " +
           "AND pm.type = 'PIX' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    List<PaymentMethod> findPixByKey(@Param("pixKey") String pixKey);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.bankAccountDetails.accountNumber = :accountNumber " +
           "AND pm.bankAccountDetails.bankCode = :bankCode " +
           "AND pm.type = 'BANK_ACCOUNT' " +
           "AND pm.deleted = false")
    Optional<PaymentMethod> findBankAccountByNumberAndBank(@Param("accountNumber") String accountNumber,
                                                          @Param("bankCode") String bankCode);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.expiresAt IS NOT NULL " +
           "AND pm.expiresAt <= :expirationDate " +
           "AND pm.status = 'ACTIVE' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.expiresAt ASC")
    List<PaymentMethod> findExpiringPaymentMethods(@Param("expirationDate") Instant expirationDate);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.lastUsedAt IS NULL OR pm.lastUsedAt < :before " +
           "AND pm.status = 'ACTIVE' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.lastUsedAt ASC NULLS FIRST")
    Page<PaymentMethod> findUnusedPaymentMethods(@Param("before") Instant before, Pageable pageable);

    @Query("SELECT COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.customer = :customer " +
           "AND pm.status = 'ACTIVE' " +
           "AND pm.deleted = false")
    long countActiveByCustomer(@Param("customer") Customer customer);

    @Query("SELECT COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.type = :type " +
           "AND pm.status = :status " +
           "AND pm.deleted = false")
    long countByTypeAndStatus(@Param("type") PaymentMethodType type,
                             @Param("status") PaymentMethodStatus status);

    @Query("SELECT pm.type, COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.status = 'ACTIVE' " +
           "AND pm.deleted = false " +
           "GROUP BY pm.type")
    List<Object[]> getPaymentMethodTypeStats();

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.createdAt >= :startDate AND pm.createdAt < :endDate " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findByDateRange(@Param("startDate") Instant startDate,
                                       @Param("endDate") Instant endDate,
                                       Pageable pageable);

    @Query("SELECT DATE(pm.createdAt), pm.type, COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.createdAt >= :startDate AND pm.createdAt < :endDate " +
           "AND pm.deleted = false " +
           "GROUP BY DATE(pm.createdAt), pm.type " +
           "ORDER BY DATE(pm.createdAt), pm.type")
    List<Object[]> getDailyPaymentMethodStats(@Param("startDate") Instant startDate,
                                             @Param("endDate") Instant endDate);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.cardDetails.brand = :brand " +
           "AND pm.type = 'CREDIT_CARD' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findCreditCardsByBrand(@Param("brand") String brand, Pageable pageable);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.bankAccountDetails.bankCode = :bankCode " +
           "AND pm.type = 'BANK_ACCOUNT' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findBankAccountsByBank(@Param("bankCode") String bankCode, Pageable pageable);

    @Query("SELECT pm.cardDetails.brand, COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.type = 'CREDIT_CARD' " +
           "AND pm.cardDetails.brand IS NOT NULL " +
           "AND pm.deleted = false " +
           "GROUP BY pm.cardDetails.brand " +
           "ORDER BY COUNT(pm) DESC")
    List<Object[]> getCreditCardBrandStats();

    @Query("SELECT pm.bankAccountDetails.bankCode, COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.type = 'BANK_ACCOUNT' " +
           "AND pm.bankAccountDetails.bankCode IS NOT NULL " +
           "AND pm.deleted = false " +
           "GROUP BY pm.bankAccountDetails.bankCode " +
           "ORDER BY COUNT(pm) DESC")
    List<Object[]> getBankAccountStats();

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.customer.id = :customerId " +
           "AND pm.type = :type " +
           "AND pm.status = 'ACTIVE' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.isDefault DESC, pm.lastUsedAt DESC NULLS LAST")
    List<PaymentMethod> findActiveByCustomerIdAndType(@Param("customerId") UUID customerId,
                                                     @Param("type") PaymentMethodType type);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.isDefault = true " +
           "AND pm.status = 'ACTIVE' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findDefaultPaymentMethods(Pageable pageable);

    @Query("SELECT " +
           "COUNT(pm) as totalPaymentMethods, " +
           "COUNT(CASE WHEN pm.status = 'ACTIVE' THEN 1 END) as activePaymentMethods, " +
           "COUNT(CASE WHEN pm.type = 'CREDIT_CARD' THEN 1 END) as creditCards, " +
           "COUNT(CASE WHEN pm.type = 'PIX' THEN 1 END) as pixKeys, " +
           "COUNT(CASE WHEN pm.type = 'BANK_ACCOUNT' THEN 1 END) as bankAccounts " +
           "FROM PaymentMethod pm WHERE " +
           "pm.createdAt >= :startDate AND pm.createdAt < :endDate " +
           "AND pm.deleted = false")
    Object getPaymentMethodSummary(@Param("startDate") Instant startDate,
                                  @Param("endDate") Instant endDate);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.usageCount > :minUsage " +
           "AND pm.status = 'ACTIVE' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.usageCount DESC, pm.lastUsedAt DESC")
    Page<PaymentMethod> findMostUsedPaymentMethods(@Param("minUsage") Integer minUsage, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PaymentMethod pm WHERE " +
           "pm.tokenId = :tokenId AND pm.deleted = false")
    boolean existsByTokenId(@Param("tokenId") String tokenId);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.customer = :customer " +
           "AND pm.type = 'PIX' " +
           "AND pm.pixDetails.pixKeyType = :keyType " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    List<PaymentMethod> findPixByCustomerAndKeyType(@Param("customer") Customer customer,
                                                   @Param("keyType") String keyType);

    @Query("SELECT COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.createdAt >= :today " +
           "AND pm.deleted = false")
    long countTodayCreated(@Param("today") Instant today);

    @Query(value = "SELECT DATE_TRUNC('hour', pm.created_at) as hour, pm.type, COUNT(*) as count " +
           "FROM payment_methods pm " +
           "WHERE pm.created_at >= :startDate AND pm.created_at < :endDate " +
           "AND pm.deleted = false " +
           "GROUP BY DATE_TRUNC('hour', pm.created_at), pm.type " +
           "ORDER BY hour, pm.type",
           nativeQuery = true)
    List<Object[]> getHourlyCreationVolume(@Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.fingerprint = :fingerprint " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    List<PaymentMethod> findByFingerprint(@Param("fingerprint") String fingerprint);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.tags LIKE CONCAT('%', :tag, '%') " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findByTag(@Param("tag") String tag, Pageable pageable);
}