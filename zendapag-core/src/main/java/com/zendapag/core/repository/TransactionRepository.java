package com.zendapag.core.repository;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Settlement;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.TransactionStatus;
import com.zendapag.core.entity.enums.TransactionType;
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

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    @Cacheable(value = "transactions", key = "#referenceId")
    Optional<Transaction> findByReferenceId(String referenceId);

    @Query("SELECT t FROM Transaction t WHERE t.merchant = :merchant AND t.deleted = false ORDER BY t.createdAt DESC")
    Page<Transaction> findByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.payment = :payment AND t.deleted = false ORDER BY t.createdAt ASC")
    List<Transaction> findByPayment(@Param("payment") Payment payment);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.merchant = :merchant AND t.type = :type " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByMerchantAndType(@Param("merchant") Merchant merchant,
                                            @Param("type") TransactionType type,
                                            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.settlement = :settlement " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findBySettlement(@Param("settlement") Settlement settlement);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                                 @Param("startDate") Instant startDate,
                                                 @Param("endDate") Instant endDate,
                                                 Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.amount >= :minAmount " +
           "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
           "AND t.deleted = false " +
           "ORDER BY t.amount DESC, t.createdAt DESC")
    Page<Transaction> findByAmountRange(@Param("minAmount") BigDecimal minAmount,
                                        @Param("maxAmount") BigDecimal maxAmount,
                                        Pageable pageable);

    @Query("SELECT SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount ELSE -t.amount END) " +
           "FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.deleted = false")
    BigDecimal calculateMerchantBalance(@Param("merchant") Merchant merchant);

    @Query("SELECT SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount ELSE -t.amount END) " +
           "FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.createdAt <= :upToDate " +
           "AND t.deleted = false")
    BigDecimal calculateMerchantBalanceUpTo(@Param("merchant") Merchant merchant,
                                            @Param("upToDate") Instant upToDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE " +
           "t.merchant = :merchant AND t.type = 'CREDIT' " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false")
    BigDecimal sumCreditsByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                                @Param("startDate") Instant startDate,
                                                @Param("endDate") Instant endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE " +
           "t.merchant = :merchant AND t.type = 'DEBIT' " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false")
    BigDecimal sumDebitsByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                               @Param("startDate") Instant startDate,
                                               @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE " +
           "t.merchant = :merchant AND t.type = :type " +
           "AND t.deleted = false")
    long countByMerchantAndType(@Param("merchant") Merchant merchant,
                                @Param("type") TransactionType type);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.settlement IS NULL " +
           "AND t.type = 'CREDIT' " +
           "AND t.merchant = :merchant " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt ASC")
    List<Transaction> findUnsettledCreditsByMerchant(@Param("merchant") Merchant merchant);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.settlement IS NULL " +
           "AND t.type = 'CREDIT' " +
           "AND t.createdAt <= :before " +
           "AND t.deleted = false " +
           "ORDER BY t.merchant, t.createdAt ASC")
    List<Transaction> findUnsettledCreditsCreatedBefore(@Param("before") Instant before);

    @Query("SELECT t.type, COUNT(t), SUM(t.amount) FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false " +
           "GROUP BY t.type")
    List<Object[]> getTransactionStatsByMerchant(@Param("merchant") Merchant merchant,
                                                 @Param("startDate") Instant startDate,
                                                 @Param("endDate") Instant endDate);

    @Query("SELECT CAST(t.createdAt AS DATE), t.type, COUNT(t), SUM(t.amount) FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false " +
           "GROUP BY CAST(t.createdAt AS DATE), t.type " +
           "ORDER BY CAST(t.createdAt AS DATE), t.type")
    List<Object[]> getDailyTransactionStatsByMerchant(@Param("merchant") Merchant merchant,
                                                      @Param("startDate") Instant startDate,
                                                      @Param("endDate") Instant endDate);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.description LIKE CONCAT('%', :searchTerm, '%') OR " +
           "t.referenceId LIKE CONCAT('%', :searchTerm, '%') " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> searchTransactions(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.amount > :threshold " +
           "AND t.deleted = false " +
           "ORDER BY t.amount DESC, t.createdAt DESC")
    Page<Transaction> findHighValueTransactions(@Param("threshold") BigDecimal threshold, Pageable pageable);

    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE " +
           "t.type = :type " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false")
    BigDecimal getAverageTransactionAmount(@Param("type") TransactionType type,
                                           @Param("startDate") Instant startDate,
                                           @Param("endDate") Instant endDate);

    @Query(value = "SELECT DATETRUNC('HOUR', t.created_at) as hour, " +
           "t.type, COUNT(*) as count, SUM(t.amount) as total " +
           "FROM transactions t " +
           "WHERE t.created_at >= :startDate AND t.created_at < :endDate " +
           "AND t.deleted = false " +
           "GROUP BY DATETRUNC('HOUR', t.created_at), t.type " +
           "ORDER BY hour, t.type",
           nativeQuery = true)
    List<Object[]> getHourlyTransactionVolume(@Param("startDate") Instant startDate,
                                              @Param("endDate") Instant endDate);

    @Query("SELECT " +
           "COUNT(t) as totalTransactions, " +
           "COUNT(CASE WHEN t.type = 'CREDIT' THEN 1 END) as creditTransactions, " +
           "COUNT(CASE WHEN t.type = 'DEBIT' THEN 1 END) as debitTransactions, " +
           "SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount ELSE 0 END) as totalCredits, " +
           "SUM(CASE WHEN t.type = 'DEBIT' THEN t.amount ELSE 0 END) as totalDebits " +
           "FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false")
    Object getTransactionSummaryByMerchant(@Param("merchant") Merchant merchant,
                                          @Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.externalId = :externalId " +
           "AND t.deleted = false")
    Optional<Transaction> findByMerchantAndExternalId(@Param("merchant") Merchant merchant,
                                                     @Param("externalId") String externalId);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.parentTransactionId = :parentId " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt ASC")
    List<Transaction> findChildTransactions(@Param("parentId") UUID parentId);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.reversalTransactionId IS NULL " +
           "AND t.type = 'DEBIT' " +
           "AND t.merchant = :merchant " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findReversibleDebitsByMerchant(@Param("merchant") Merchant merchant);

    @Query("SELECT " +
           "m.name as merchantName, " +
           "COUNT(t) as transactionCount, " +
           "SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount ELSE 0 END) as totalCredits, " +
           "SUM(CASE WHEN t.type = 'DEBIT' THEN t.amount ELSE 0 END) as totalDebits " +
           "FROM Transaction t JOIN t.merchant m " +
           "WHERE t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false " +
           "GROUP BY m.id, m.name " +
           "ORDER BY transactionCount DESC")
    List<Object[]> getMerchantTransactionStats(@Param("startDate") Instant startDate,
                                              @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.createdAt >= :today " +
           "AND t.deleted = false")
    long countTodayTransactionsByMerchant(@Param("merchant") Merchant merchant,
                                         @Param("today") Instant today);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.gatewayTransactionId = :gatewayTransactionId " +
           "AND t.deleted = false")
    Optional<Transaction> findByGatewayTransactionId(@Param("gatewayTransactionId") String gatewayTransactionId);

    @Query("SELECT SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount ELSE -t.amount END) " +
           "FROM Transaction t WHERE " +
           "t.settlement = :settlement " +
           "AND t.deleted = false")
    BigDecimal calculateSettlementAmount(@Param("settlement") Settlement settlement);

    // NEW: findByAccount (for TransactionService)
    @Query("SELECT t FROM Transaction t WHERE t.account = :account AND t.deleted = false ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccount(@Param("account") Account account, Pageable pageable);

    // NEW: findByMerchantAndStatus (for TransactionService)
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.status = :status " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                              @Param("status") TransactionStatus status,
                                              Pageable pageable);

    // NEW: findByMerchantAndCreatedAtBetween with Pageable (for TransactionService)
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByMerchantAndCreatedAtBetween(@Param("merchant") Merchant merchant,
                                                        @Param("startDate") Instant startDate,
                                                        @Param("endDate") Instant endDate,
                                                        Pageable pageable);

    // NEW: findByMerchantAndCreatedAtBetween without Pageable (for ReportService)
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByMerchantAndCreatedAtBetween(@Param("merchant") Merchant merchant,
                                                        @Param("startDate") Instant startDate,
                                                        @Param("endDate") Instant endDate);

    // NEW: sumCreditsByAccount (for TransactionService)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
           "t.account = :account " +
           "AND t.type IN ('PAYMENT', 'REVERSAL') " +
           "AND t.deleted = false")
    BigDecimal sumCreditsByAccount(@Param("account") Account account);

    // NEW: sumDebitsByAccount (for TransactionService)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
           "t.account = :account " +
           "AND t.type IN ('REFUND', 'CHARGEBACK', 'FEE', 'SETTLEMENT') " +
           "AND t.deleted = false")
    BigDecimal sumDebitsByAccount(@Param("account") Account account);

    // NEW: countByMerchantAndCreatedAtBetween (for TransactionService)
    @Query("SELECT COUNT(t) FROM Transaction t WHERE " +
           "t.merchant = :merchant " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "AND t.deleted = false")
    long countByMerchantAndCreatedAtBetween(@Param("merchant") Merchant merchant,
                                            @Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate);
}
