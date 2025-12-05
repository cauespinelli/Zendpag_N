package com.zendapag.core.repository;

import com.zendapag.core.entity.Customer;
import com.zendapag.core.entity.PaymentMethod;
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

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.customer = :customer AND pm.deleted = false ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findByCustomer(@Param("customer") Customer customer, Pageable pageable);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.customer = :customer AND pm.deleted = false ORDER BY pm.createdAt DESC")
    List<PaymentMethod> findByCustomerList(@Param("customer") Customer customer);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.customer = :customer " +
           "AND pm.type = :type " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    List<PaymentMethod> findByCustomerAndType(@Param("customer") Customer customer,
                                             @Param("type") PaymentMethodType type);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.customer = :customer " +
           "AND pm.active = true " +
           "AND pm.deleted = false")
    List<PaymentMethod> findActiveByCustomer(@Param("customer") Customer customer);

    @Cacheable(value = "paymentMethods", key = "#token")
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.token = :token AND pm.deleted = false")
    Optional<PaymentMethod> findByToken(@Param("token") String token);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.type = :type " +
           "AND pm.active = :active " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findByTypeAndActive(@Param("type") PaymentMethodType type,
                                            @Param("active") Boolean active,
                                            Pageable pageable);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.active = :active " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findByActive(@Param("active") Boolean active, Pageable pageable);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.lastFour = :lastFour " +
           "AND pm.type = 'CREDIT_CARD' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    List<PaymentMethod> findCreditCardsByLastFour(@Param("lastFour") String lastFour);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.pixKey = :pixKey " +
           "AND pm.type = 'PIX' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    List<PaymentMethod> findPixByKey(@Param("pixKey") String pixKey);

    @Query("SELECT COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.customer = :customer " +
           "AND pm.active = true " +
           "AND pm.deleted = false")
    long countActiveByCustomer(@Param("customer") Customer customer);

    @Query("SELECT pm.type, COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.active = true " +
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

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.brand = :brand " +
           "AND pm.type = 'CREDIT_CARD' " +
           "AND pm.deleted = false " +
           "ORDER BY pm.createdAt DESC")
    Page<PaymentMethod> findCreditCardsByBrand(@Param("brand") String brand, Pageable pageable);

    @Query("SELECT pm.brand, COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.type = 'CREDIT_CARD' " +
           "AND pm.brand IS NOT NULL " +
           "AND pm.deleted = false " +
           "GROUP BY pm.brand " +
           "ORDER BY COUNT(pm) DESC")
    List<Object[]> getCreditCardBrandStats();

    @Query("SELECT pm.bankCode, COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.type = 'BANK_TRANSFER' " +
           "AND pm.bankCode IS NOT NULL " +
           "AND pm.deleted = false " +
           "GROUP BY pm.bankCode " +
           "ORDER BY COUNT(pm) DESC")
    List<Object[]> getBankAccountStats();

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.usageCount > :minUsage " +
           "AND pm.active = true " +
           "AND pm.deleted = false " +
           "ORDER BY pm.usageCount DESC")
    Page<PaymentMethod> findMostUsedPaymentMethods(@Param("minUsage") Long minUsage, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PaymentMethod pm WHERE " +
           "pm.token = :token AND pm.deleted = false")
    boolean existsByToken(@Param("token") String token);

    @Query("SELECT COUNT(pm) FROM PaymentMethod pm WHERE " +
           "pm.createdAt >= :today " +
           "AND pm.deleted = false")
    long countTodayCreated(@Param("today") Instant today);
}
