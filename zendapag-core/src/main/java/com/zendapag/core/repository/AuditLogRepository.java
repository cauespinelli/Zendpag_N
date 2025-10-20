package com.zendapag.core.repository;

import com.zendapag.core.entity.AuditLog;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.enums.AuditAction;
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
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT al FROM AuditLog al WHERE al.merchant = :merchant ORDER BY al.timestamp DESC")
    Page<AuditLog> findByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.entityType = :entityType " +
           "AND al.entityId = :entityId " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByEntity(@Param("entityType") String entityType,
                               @Param("entityId") String entityId,
                               Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.entityType = :entityType " +
           "AND al.entityId = :entityId " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByMerchantAndEntity(@Param("merchant") Merchant merchant,
                                          @Param("entityType") String entityType,
                                          @Param("entityId") String entityId,
                                          Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.action = :action " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByAction(@Param("action") AuditAction action, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.action = :action " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByMerchantAndAction(@Param("merchant") Merchant merchant,
                                          @Param("action") AuditAction action,
                                          Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.userId = :userId " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.userId = :userId " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByMerchantAndUserId(@Param("merchant") Merchant merchant,
                                          @Param("userId") String userId,
                                          Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.timestamp >= :startDate AND al.timestamp < :endDate " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByDateRange(@Param("startDate") Instant startDate,
                                  @Param("endDate") Instant endDate,
                                  Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.timestamp >= :startDate AND al.timestamp < :endDate " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                             @Param("startDate") Instant startDate,
                                             @Param("endDate") Instant endDate,
                                             Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.ipAddress = :ipAddress " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByIpAddress(@Param("ipAddress") String ipAddress, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.suspicious = true " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findSuspiciousActivities(Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.suspicious = true " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findSuspiciousActivitiesByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.success = false " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findFailedOperations(Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.success = false " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findFailedOperationsByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.riskScore IS NOT NULL " +
           "AND al.riskScore >= :minScore " +
           "ORDER BY al.riskScore DESC, al.timestamp DESC")
    Page<AuditLog> findHighRiskActivities(@Param("minScore") Integer minScore, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.sessionId = :sessionId " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.correlationId = :correlationId " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findByCorrelationId(@Param("correlationId") String correlationId);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.requestId = :requestId " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findByRequestId(@Param("requestId") String requestId);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.apiKeyId = :apiKeyId " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByApiKeyId(@Param("apiKeyId") String apiKeyId, Pageable pageable);

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.action = :action " +
           "AND al.timestamp >= :startDate AND al.timestamp < :endDate")
    long countByMerchantAndActionAndDateRange(@Param("merchant") Merchant merchant,
                                             @Param("action") AuditAction action,
                                             @Param("startDate") Instant startDate,
                                             @Param("endDate") Instant endDate);

    @Query("SELECT al.action, COUNT(al) FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.timestamp >= :startDate AND al.timestamp < :endDate " +
           "GROUP BY al.action " +
           "ORDER BY COUNT(al) DESC")
    List<Object[]> getActionStatsByMerchant(@Param("merchant") Merchant merchant,
                                           @Param("startDate") Instant startDate,
                                           @Param("endDate") Instant endDate);

    @Query("SELECT al.entityType, COUNT(al) FROM AuditLog al WHERE " +
           "al.timestamp >= :startDate AND al.timestamp < :endDate " +
           "GROUP BY al.entityType " +
           "ORDER BY COUNT(al) DESC")
    List<Object[]> getEntityTypeStats(@Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate);

    @Query("SELECT DATE(al.timestamp), COUNT(al) FROM AuditLog al WHERE " +
           "al.timestamp >= :startDate AND al.timestamp < :endDate " +
           "GROUP BY DATE(al.timestamp) " +
           "ORDER BY DATE(al.timestamp)")
    List<Object[]> getDailyAuditStats(@Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.description LIKE CONCAT('%', :searchTerm, '%') OR " +
           "al.userId LIKE CONCAT('%', :searchTerm, '%') OR " +
           "al.userName LIKE CONCAT('%', :searchTerm, '%') OR " +
           "al.entityId LIKE CONCAT('%', :searchTerm, '%') " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> searchAuditLogs(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.ipAddress = :ipAddress " +
           "AND al.timestamp >= :since " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentActivitiesByIpAddress(@Param("ipAddress") String ipAddress,
                                                  @Param("since") Instant since);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.userId = :userId " +
           "AND al.timestamp >= :since " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentActivitiesByUserId(@Param("userId") String userId,
                                               @Param("since") Instant since);

    @Query("SELECT " +
           "COUNT(al) as totalLogs, " +
           "COUNT(CASE WHEN al.success = true THEN 1 END) as successfulOperations, " +
           "COUNT(CASE WHEN al.success = false THEN 1 END) as failedOperations, " +
           "COUNT(CASE WHEN al.suspicious = true THEN 1 END) as suspiciousActivities, " +
           "COUNT(DISTINCT al.userId) as uniqueUsers, " +
           "COUNT(DISTINCT al.ipAddress) as uniqueIpAddresses " +
           "FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.timestamp >= :startDate AND al.timestamp < :endDate")
    Object getAuditSummaryByMerchant(@Param("merchant") Merchant merchant,
                                    @Param("startDate") Instant startDate,
                                    @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE " +
           "al.timestamp >= :today")
    long countTodayLogs(@Param("today") Instant today);

    @Query("SELECT al.application, COUNT(al) FROM AuditLog al WHERE " +
           "al.application IS NOT NULL " +
           "AND al.timestamp >= :startDate AND al.timestamp < :endDate " +
           "GROUP BY al.application " +
           "ORDER BY COUNT(al) DESC")
    List<Object[]> getApplicationStats(@Param("startDate") Instant startDate,
                                      @Param("endDate") Instant endDate);

    @Query("SELECT al.userType, COUNT(al) FROM AuditLog al WHERE " +
           "al.userType IS NOT NULL " +
           "AND al.timestamp >= :startDate AND al.timestamp < :endDate " +
           "GROUP BY al.userType " +
           "ORDER BY COUNT(al) DESC")
    List<Object[]> getUserTypeStats(@Param("startDate") Instant startDate,
                                   @Param("endDate") Instant endDate);

    @Query(value = "SELECT DATE_TRUNC('hour', al.timestamp) as hour, COUNT(*) as count " +
           "FROM audit_logs al " +
           "WHERE al.timestamp >= :startDate AND al.timestamp < :endDate " +
           "GROUP BY DATE_TRUNC('hour', al.timestamp) " +
           "ORDER BY hour",
           nativeQuery = true)
    List<Object[]> getHourlyAuditVolume(@Param("startDate") Instant startDate,
                                       @Param("endDate") Instant endDate);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.tags LIKE CONCAT('%', :tag, '%') " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByTag(@Param("tag") String tag, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.feature = :feature " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByFeature(@Param("feature") String feature, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.module = :module " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByModule(@Param("module") String module, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT al.ipAddress) FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.timestamp >= :startDate AND al.timestamp < :endDate")
    long countDistinctIpAddressesByMerchant(@Param("merchant") Merchant merchant,
                                           @Param("startDate") Instant startDate,
                                           @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(DISTINCT al.userId) FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.userId IS NOT NULL " +
           "AND al.timestamp >= :startDate AND al.timestamp < :endDate")
    long countDistinctUsersByMerchant(@Param("merchant") Merchant merchant,
                                     @Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.processingTimeMs > :threshold " +
           "ORDER BY al.processingTimeMs DESC, al.timestamp DESC")
    Page<AuditLog> findSlowOperations(@Param("threshold") Long threshold, Pageable pageable);

    @Query("SELECT AVG(al.processingTimeMs) FROM AuditLog al WHERE " +
           "al.processingTimeMs IS NOT NULL " +
           "AND al.timestamp >= :startDate AND al.timestamp < :endDate")
    Double getAverageProcessingTime(@Param("startDate") Instant startDate,
                                   @Param("endDate") Instant endDate);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.merchant = :merchant " +
           "AND al.entityType = :entityType " +
           "AND al.entityId = :entityId " +
           "AND al.action = :action " +
           "ORDER BY al.timestamp DESC")
    Optional<AuditLog> findLastByMerchantAndEntityAndAction(@Param("merchant") Merchant merchant,
                                                           @Param("entityType") String entityType,
                                                           @Param("entityId") String entityId,
                                                           @Param("action") AuditAction action);
}