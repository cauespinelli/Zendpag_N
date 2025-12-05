package com.zendapag.core.repository;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Webhook;
import com.zendapag.core.entity.enums.WebhookEventType;
import com.zendapag.core.entity.enums.WebhookStatus;
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
public interface WebhookRepository extends JpaRepository<Webhook, UUID>, JpaSpecificationExecutor<Webhook> {

    @Query("SELECT w FROM Webhook w WHERE w.merchant = :merchant AND w.deleted = false ORDER BY w.createdAt DESC")
    Page<Webhook> findByMerchant(@Param("merchant") Merchant merchant, Pageable pageable);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.merchant = :merchant " +
           "AND w.eventType = :eventType " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Page<Webhook> findByMerchantAndEventType(@Param("merchant") Merchant merchant,
                                            @Param("eventType") WebhookEventType eventType,
                                            Pageable pageable);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.status = :status " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt ASC")
    Page<Webhook> findByStatus(@Param("status") WebhookStatus status, Pageable pageable);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.status = 'PENDING' " +
           "AND w.nextRetryAt <= :now " +
           "AND w.deleted = false " +
           "ORDER BY w.nextRetryAt ASC")
    List<Webhook> findPendingWebhooksReadyForRetry(@Param("now") Instant now);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.status = 'FAILED' " +
           "AND w.retryCount < w.maxRetries " +
           "AND w.nextRetryAt <= :now " +
           "AND w.deleted = false " +
           "ORDER BY w.nextRetryAt ASC")
    List<Webhook> findFailedWebhooksReadyForRetry(@Param("now") Instant now);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.entityId = :entityId " +
           "AND w.eventType = :eventType " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    List<Webhook> findByEntityIdAndEventType(@Param("entityId") String entityId,
                                            @Param("eventType") WebhookEventType eventType);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.merchant = :merchant " +
           "AND w.entityId = :entityId " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    List<Webhook> findByMerchantAndEntityId(@Param("merchant") Merchant merchant,
                                           @Param("entityId") String entityId);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.createdAt >= :startDate AND w.createdAt < :endDate " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Page<Webhook> findByDateRange(@Param("startDate") Instant startDate,
                                 @Param("endDate") Instant endDate,
                                 Pageable pageable);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.merchant = :merchant " +
           "AND w.createdAt >= :startDate AND w.createdAt < :endDate " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Page<Webhook> findByMerchantAndDateRange(@Param("merchant") Merchant merchant,
                                            @Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate,
                                            Pageable pageable);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.retryCount >= :minRetries " +
           "AND w.deleted = false " +
           "ORDER BY w.retryCount DESC, w.createdAt DESC")
    Page<Webhook> findWithHighRetryCount(@Param("minRetries") Integer minRetries, Pageable pageable);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.status = 'FAILED' " +
           "AND w.retryCount >= w.maxRetries " +
           "AND w.deleted = false " +
           "ORDER BY w.updatedAt DESC")
    Page<Webhook> findExhaustedRetryWebhooks(Pageable pageable);

    @Query("SELECT COUNT(w) FROM Webhook w WHERE " +
           "w.merchant = :merchant " +
           "AND w.status = :status " +
           "AND w.deleted = false")
    long countByMerchantAndStatus(@Param("merchant") Merchant merchant,
                                 @Param("status") WebhookStatus status);

    @Query("SELECT COUNT(w) FROM Webhook w WHERE " +
           "w.eventType = :eventType " +
           "AND w.status = :status " +
           "AND w.deleted = false")
    long countByEventTypeAndStatus(@Param("eventType") WebhookEventType eventType,
                                  @Param("status") WebhookStatus status);

    @Query("SELECT w.status, COUNT(w) FROM Webhook w WHERE " +
           "w.merchant = :merchant " +
           "AND w.createdAt >= :startDate AND w.createdAt < :endDate " +
           "AND w.deleted = false " +
           "GROUP BY w.status")
    List<Object[]> getWebhookStatsByMerchant(@Param("merchant") Merchant merchant,
                                            @Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate);

    @Query("SELECT w.eventType, COUNT(w) FROM Webhook w WHERE " +
           "w.createdAt >= :startDate AND w.createdAt < :endDate " +
           "AND w.deleted = false " +
           "GROUP BY w.eventType " +
           "ORDER BY COUNT(w) DESC")
    List<Object[]> getEventTypeStats(@Param("startDate") Instant startDate,
                                    @Param("endDate") Instant endDate);

    @Query("SELECT CAST(w.createdAt AS DATE), w.status, COUNT(w) FROM Webhook w WHERE " +
           "w.createdAt >= :startDate AND w.createdAt < :endDate " +
           "AND w.deleted = false " +
           "GROUP BY CAST(w.createdAt AS DATE), w.status " +
           "ORDER BY CAST(w.createdAt AS DATE), w.status")
    List<Object[]> getDailyWebhookStats(@Param("startDate") Instant startDate,
                                       @Param("endDate") Instant endDate);

    // H2 compatible: using native query with DATEDIFF
    @Query(value = "SELECT AVG(DATEDIFF(MILLISECOND, w.created_at, w.delivered_at)) FROM webhooks w WHERE " +
           "w.status = 'SUCCESS' " +
           "AND w.delivered_at IS NOT NULL " +
           "AND w.created_at >= :startDate AND w.created_at < :endDate " +
           "AND w.deleted = false", nativeQuery = true)
    Double getAverageDeliveryTime(@Param("startDate") Instant startDate,
                                 @Param("endDate") Instant endDate);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.url = :url " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Page<Webhook> findByUrl(@Param("url") String url, Pageable pageable);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.httpStatusCode = :statusCode " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Page<Webhook> findByHttpStatusCode(@Param("statusCode") Integer statusCode, Pageable pageable);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.responseTime > :threshold " +
           "AND w.deleted = false " +
           "ORDER BY w.responseTime DESC, w.createdAt DESC")
    Page<Webhook> findSlowWebhooks(@Param("threshold") Long threshold, Pageable pageable);

    // H2 compatible: using native query with DATEDIFF
    @Query(value = "SELECT " +
           "COUNT(*) as totalWebhooks, " +
           "COUNT(CASE WHEN w.status = 'SUCCESS' THEN 1 END) as successWebhooks, " +
           "COUNT(CASE WHEN w.status = 'FAILED' THEN 1 END) as failedWebhooks, " +
           "COUNT(CASE WHEN w.status = 'PENDING' THEN 1 END) as pendingWebhooks, " +
           "AVG(CASE WHEN w.delivered_at IS NOT NULL THEN DATEDIFF(MILLISECOND, w.created_at, w.delivered_at) END) as avgDeliveryTime " +
           "FROM webhooks w WHERE " +
           "w.merchant_id = :#{#merchant.id} " +
           "AND w.created_at >= :startDate AND w.created_at < :endDate " +
           "AND w.deleted = false", nativeQuery = true)
    Object getWebhookSummaryByMerchant(@Param("merchant") Merchant merchant,
                                      @Param("startDate") Instant startDate,
                                      @Param("endDate") Instant endDate);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.correlationId = :correlationId " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    List<Webhook> findByCorrelationId(@Param("correlationId") String correlationId);

    @Query("SELECT COUNT(w) FROM Webhook w WHERE " +
           "w.createdAt >= :today " +
           "AND w.deleted = false")
    long countTodayWebhooks(@Param("today") Instant today);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.merchant = :merchant " +
           "AND w.eventType = :eventType " +
           "AND w.entityId = :entityId " +
           "AND w.status = 'SUCCESS' " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Optional<Webhook> findLastSuccessfulDelivery(@Param("merchant") Merchant merchant,
                                                @Param("eventType") WebhookEventType eventType,
                                                @Param("entityId") String entityId);

    @Query(value = "SELECT DATETRUNC('HOUR', w.created_at) as hour, " +
           "w.status, COUNT(*) as count " +
           "FROM webhooks w " +
           "WHERE w.created_at >= :startDate AND w.created_at < :endDate " +
           "AND w.deleted = false " +
           "GROUP BY DATETRUNC('HOUR', w.created_at), w.status " +
           "ORDER BY hour, w.status",
           nativeQuery = true)
    List<Object[]> getHourlyWebhookVolume(@Param("startDate") Instant startDate,
                                         @Param("endDate") Instant endDate);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.merchant = :merchant " +
           "AND w.status IN ('FAILED', 'PENDING') " +
           "AND w.createdAt >= :since " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    List<Webhook> findRecentFailuresForMerchant(@Param("merchant") Merchant merchant,
                                               @Param("since") Instant since);

    @Query("SELECT w.httpStatusCode, COUNT(w) FROM Webhook w WHERE " +
           "w.httpStatusCode IS NOT NULL " +
           "AND w.createdAt >= :startDate AND w.createdAt < :endDate " +
           "AND w.deleted = false " +
           "GROUP BY w.httpStatusCode " +
           "ORDER BY COUNT(w) DESC")
    List<Object[]> getHttpStatusCodeStats(@Param("startDate") Instant startDate,
                                         @Param("endDate") Instant endDate);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.signature = :signature " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Optional<Webhook> findBySignature(@Param("signature") String signature);

    @Query("SELECT w FROM Webhook w WHERE " +
           "w.tags LIKE CONCAT('%', :tag, '%') " +
           "AND w.deleted = false " +
           "ORDER BY w.createdAt DESC")
    Page<Webhook> findByTag(@Param("tag") String tag, Pageable pageable);
}