package com.zendapag.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Dead Letter Queue configuration for failed webhook deliveries
 * Implements sophisticated DLQ handling with retry scheduling and manual recovery
 */
@Configuration
public class WebhookDLQConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDLQConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${zendapag.webhook.dlq.topic:webhook-dead-letter-queue}")
    private String dlqTopic;

    @Value("${zendapag.webhook.dlq.retention-hours:168}") // 7 days
    private int dlqRetentionHours;

    /**
     * Producer factory for DLQ messages
     */
    @Bean("webhookDLQProducerFactory")
    public ProducerFactory<String, Object> webhookDLQProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // DLQ specific configuration - optimize for reliability over performance
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 10);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 300000); // 5 minutes

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for DLQ operations
     */
    @Bean("webhookDLQTemplate")
    public KafkaTemplate<String, Object> webhookDLQTemplate() {
        return new KafkaTemplate<>(webhookDLQProducerFactory());
    }

    /**
     * Dead Letter Queue manager
     */
    @Component
    public static class WebhookDLQManager {
        private static final Logger dlqLogger = LoggerFactory.getLogger("WebhookDLQ");

        private final KafkaTemplate<String, Object> dlqTemplate;
        private final RedisTemplate<String, Object> redisTemplate;
        private final ObjectMapper objectMapper;

        @Value("${zendapag.webhook.dlq.topic:webhook-dead-letter-queue}")
        private String dlqTopic;

        @Value("${zendapag.webhook.dlq.redis-prefix:webhook:dlq}")
        private String redisPrefix;

        public WebhookDLQManager(KafkaTemplate<String, Object> dlqTemplate,
                               RedisTemplate<String, Object> redisTemplate,
                               ObjectMapper objectMapper) {
            this.dlqTemplate = dlqTemplate;
            this.redisTemplate = redisTemplate;
            this.objectMapper = objectMapper;
        }

        /**
         * Send failed webhook to Dead Letter Queue
         */
        public void sendToDLQ(String webhookId, String merchantId, String payload,
                             String endpoint, String errorReason, int attemptCount,
                             Instant originalTimestamp) {

            try {
                WebhookDLQMessage dlqMessage = WebhookDLQMessage.builder()
                    .webhookId(webhookId)
                    .merchantId(merchantId)
                    .payload(payload)
                    .endpoint(endpoint)
                    .errorReason(errorReason)
                    .attemptCount(attemptCount)
                    .originalTimestamp(originalTimestamp)
                    .dlqTimestamp(Instant.now())
                    .status(WebhookDLQStatus.FAILED)
                    .build();

                // Send to Kafka DLQ topic
                dlqTemplate.send(dlqTopic, webhookId, dlqMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            dlqLogger.info("Successfully sent webhook {} to DLQ after {} attempts",
                                webhookId, attemptCount);

                            // Store in Redis for tracking and potential manual processing
                            storeDLQMessageInRedis(dlqMessage);

                        } else {
                            dlqLogger.error("Failed to send webhook {} to DLQ", webhookId, ex);
                        }
                    });

            } catch (Exception e) {
                dlqLogger.error("Error sending webhook {} to DLQ", webhookId, e);
            }
        }

        /**
         * Store DLQ message in Redis for tracking
         */
        private void storeDLQMessageInRedis(WebhookDLQMessage dlqMessage) {
            try {
                String redisKey = redisPrefix + ":messages:" + dlqMessage.getWebhookId();

                Map<String, Object> messageData = new HashMap<>();
                messageData.put("webhookId", dlqMessage.getWebhookId());
                messageData.put("merchantId", dlqMessage.getMerchantId());
                messageData.put("endpoint", dlqMessage.getEndpoint());
                messageData.put("errorReason", dlqMessage.getErrorReason());
                messageData.put("attemptCount", dlqMessage.getAttemptCount());
                messageData.put("originalTimestamp", dlqMessage.getOriginalTimestamp().getEpochSecond());
                messageData.put("dlqTimestamp", dlqMessage.getDlqTimestamp().getEpochSecond());
                messageData.put("status", dlqMessage.getStatus().name());

                redisTemplate.opsForHash().putAll(redisKey, messageData);

                // Set expiration (default 7 days)
                redisTemplate.expire(redisKey, Duration.ofHours(168));

                // Add to merchant-specific set for querying
                String merchantKey = redisPrefix + ":by_merchant:" + dlqMessage.getMerchantId();
                redisTemplate.opsForSet().add(merchantKey, dlqMessage.getWebhookId());
                redisTemplate.expire(merchantKey, Duration.ofHours(168));

                // Add to global DLQ tracking
                redisTemplate.opsForZSet().add(
                    redisPrefix + ":timeline",
                    dlqMessage.getWebhookId(),
                    dlqMessage.getDlqTimestamp().getEpochSecond()
                );

                dlqLogger.debug("Stored DLQ message {} in Redis", dlqMessage.getWebhookId());

            } catch (Exception e) {
                dlqLogger.error("Error storing DLQ message in Redis: {}", dlqMessage.getWebhookId(), e);
            }
        }

        /**
         * Get DLQ messages for merchant
         */
        public Set<Object> getDLQMessagesForMerchant(String merchantId) {
            try {
                String merchantKey = redisPrefix + ":by_merchant:" + merchantId;
                return redisTemplate.opsForSet().members(merchantKey);
            } catch (Exception e) {
                dlqLogger.error("Error getting DLQ messages for merchant: {}", merchantId, e);
                return Set.of();
            }
        }

        /**
         * Get DLQ message details
         */
        public WebhookDLQMessage getDLQMessage(String webhookId) {
            try {
                String redisKey = redisPrefix + ":messages:" + webhookId;
                Map<Object, Object> messageData = redisTemplate.opsForHash().entries(redisKey);

                if (messageData.isEmpty()) {
                    return null;
                }

                return WebhookDLQMessage.builder()
                    .webhookId((String) messageData.get("webhookId"))
                    .merchantId((String) messageData.get("merchantId"))
                    .endpoint((String) messageData.get("endpoint"))
                    .errorReason((String) messageData.get("errorReason"))
                    .attemptCount((Integer) messageData.get("attemptCount"))
                    .originalTimestamp(Instant.ofEpochSecond((Long) messageData.get("originalTimestamp")))
                    .dlqTimestamp(Instant.ofEpochSecond((Long) messageData.get("dlqTimestamp")))
                    .status(WebhookDLQStatus.valueOf((String) messageData.get("status")))
                    .build();

            } catch (Exception e) {
                dlqLogger.error("Error getting DLQ message: {}", webhookId, e);
                return null;
            }
        }

        /**
         * Mark DLQ message for manual retry
         */
        public boolean markForRetry(String webhookId) {
            try {
                WebhookDLQMessage dlqMessage = getDLQMessage(webhookId);
                if (dlqMessage == null) {
                    dlqLogger.warn("DLQ message not found: {}", webhookId);
                    return false;
                }

                // Update status to RETRY_REQUESTED
                String redisKey = redisPrefix + ":messages:" + webhookId;
                redisTemplate.opsForHash().put(redisKey, "status", WebhookDLQStatus.RETRY_REQUESTED.name());
                redisTemplate.opsForHash().put(redisKey, "retryRequestedAt", Instant.now().getEpochSecond());

                dlqLogger.info("Marked DLQ message {} for retry", webhookId);
                return true;

            } catch (Exception e) {
                dlqLogger.error("Error marking DLQ message for retry: {}", webhookId, e);
                return false;
            }
        }

        /**
         * Get DLQ statistics
         */
        public WebhookDLQStats getStatistics() {
            try {
                // Count messages by status from Redis
                Set<String> allKeys = redisTemplate.keys(redisPrefix + ":messages:*");
                int totalMessages = allKeys != null ? allKeys.size() : 0;

                // Count by merchant
                Set<String> merchantKeys = redisTemplate.keys(redisPrefix + ":by_merchant:*");
                int affectedMerchants = merchantKeys != null ? merchantKeys.size() : 0;

                // Get timeline data for trend analysis
                Set<Object> recentMessages = redisTemplate.opsForZSet()
                    .reverseRangeByScore(redisPrefix + ":timeline",
                        Instant.now().minus(Duration.ofHours(24)).getEpochSecond(),
                        Instant.now().getEpochSecond());

                int messagesLast24h = recentMessages != null ? recentMessages.size() : 0;

                return WebhookDLQStats.builder()
                    .totalMessages(totalMessages)
                    .affectedMerchants(affectedMerchants)
                    .messagesLast24Hours(messagesLast24h)
                    .oldestMessageTimestamp(getOldestMessageTimestamp())
                    .build();

            } catch (Exception e) {
                dlqLogger.error("Error getting DLQ statistics", e);
                return WebhookDLQStats.builder().build();
            }
        }

        private Instant getOldestMessageTimestamp() {
            try {
                Set<Object> oldest = redisTemplate.opsForZSet()
                    .range(redisPrefix + ":timeline", 0, 0);

                if (oldest != null && !oldest.isEmpty()) {
                    String webhookId = (String) oldest.iterator().next();
                    WebhookDLQMessage message = getDLQMessage(webhookId);
                    return message != null ? message.getDlqTimestamp() : null;
                }
            } catch (Exception e) {
                dlqLogger.error("Error getting oldest message timestamp", e);
            }
            return null;
        }
    }

    /**
     * DLQ message consumer for monitoring and potential reprocessing
     */
    @Component
    public static class WebhookDLQConsumer {
        private static final Logger consumerLogger = LoggerFactory.getLogger("WebhookDLQConsumer");

        @KafkaListener(
            topics = "${zendapag.webhook.dlq.topic:webhook-dead-letter-queue}",
            groupId = "webhook-dlq-monitor"
        )
        public void consumeDLQMessage(@Payload WebhookDLQMessage dlqMessage,
                                     @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

            consumerLogger.info("Received DLQ message: webhook={}, merchant={}, error={}",
                dlqMessage.getWebhookId(),
                dlqMessage.getMerchantId(),
                dlqMessage.getErrorReason());

            // Additional processing like alerting, logging to external systems, etc.
            // This consumer mainly serves monitoring purposes
        }
    }

    /**
     * Scheduled cleanup of old DLQ messages
     */
    @Component
    public static class WebhookDLQCleaner {
        private static final Logger cleanerLogger = LoggerFactory.getLogger("WebhookDLQCleaner");

        private final RedisTemplate<String, Object> redisTemplate;

        @Value("${zendapag.webhook.dlq.redis-prefix:webhook:dlq}")
        private String redisPrefix;

        @Value("${zendapag.webhook.dlq.retention-hours:168}") // 7 days
        private int retentionHours;

        public WebhookDLQCleaner(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
        public void cleanupExpiredMessages() {
            try {
                long cutoffTimestamp = Instant.now()
                    .minus(Duration.ofHours(retentionHours))
                    .getEpochSecond();

                // Remove old messages from timeline
                Long removedCount = redisTemplate.opsForZSet()
                    .removeRangeByScore(redisPrefix + ":timeline", 0, cutoffTimestamp);

                if (removedCount != null && removedCount > 0) {
                    cleanerLogger.info("Cleaned up {} expired DLQ messages", removedCount);
                }

                // Clean up empty merchant sets
                cleanupEmptyMerchantSets();

            } catch (Exception e) {
                cleanerLogger.error("Error cleaning up expired DLQ messages", e);
            }
        }

        private void cleanupEmptyMerchantSets() {
            try {
                Set<String> merchantKeys = redisTemplate.keys(redisPrefix + ":by_merchant:*");
                if (merchantKeys == null) return;

                for (String key : merchantKeys) {
                    Long size = redisTemplate.opsForSet().size(key);
                    if (size != null && size == 0) {
                        redisTemplate.delete(key);
                    }
                }
            } catch (Exception e) {
                cleanerLogger.error("Error cleaning up empty merchant sets", e);
            }
        }
    }

    /**
     * DLQ message data structure
     */
    public static class WebhookDLQMessage {
        private String webhookId;
        private String merchantId;
        private String payload;
        private String endpoint;
        private String errorReason;
        private int attemptCount;
        private Instant originalTimestamp;
        private Instant dlqTimestamp;
        private WebhookDLQStatus status;

        public static Builder builder() {
            return new Builder();
        }

        // Getters and setters
        public String getWebhookId() { return webhookId; }
        public String getMerchantId() { return merchantId; }
        public String getPayload() { return payload; }
        public String getEndpoint() { return endpoint; }
        public String getErrorReason() { return errorReason; }
        public int getAttemptCount() { return attemptCount; }
        public Instant getOriginalTimestamp() { return originalTimestamp; }
        public Instant getDlqTimestamp() { return dlqTimestamp; }
        public WebhookDLQStatus getStatus() { return status; }

        public static class Builder {
            private WebhookDLQMessage message = new WebhookDLQMessage();

            public Builder webhookId(String webhookId) {
                message.webhookId = webhookId;
                return this;
            }

            public Builder merchantId(String merchantId) {
                message.merchantId = merchantId;
                return this;
            }

            public Builder payload(String payload) {
                message.payload = payload;
                return this;
            }

            public Builder endpoint(String endpoint) {
                message.endpoint = endpoint;
                return this;
            }

            public Builder errorReason(String errorReason) {
                message.errorReason = errorReason;
                return this;
            }

            public Builder attemptCount(int attemptCount) {
                message.attemptCount = attemptCount;
                return this;
            }

            public Builder originalTimestamp(Instant originalTimestamp) {
                message.originalTimestamp = originalTimestamp;
                return this;
            }

            public Builder dlqTimestamp(Instant dlqTimestamp) {
                message.dlqTimestamp = dlqTimestamp;
                return this;
            }

            public Builder status(WebhookDLQStatus status) {
                message.status = status;
                return this;
            }

            public WebhookDLQMessage build() {
                return message;
            }
        }
    }

    /**
     * DLQ status enumeration
     */
    public enum WebhookDLQStatus {
        FAILED,
        RETRY_REQUESTED,
        REPROCESSING,
        RESOLVED,
        IGNORED
    }

    /**
     * DLQ statistics
     */
    public static class WebhookDLQStats {
        private int totalMessages;
        private int affectedMerchants;
        private int messagesLast24Hours;
        private Instant oldestMessageTimestamp;

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public int getTotalMessages() { return totalMessages; }
        public int getAffectedMerchants() { return affectedMerchants; }
        public int getMessagesLast24Hours() { return messagesLast24Hours; }
        public Instant getOldestMessageTimestamp() { return oldestMessageTimestamp; }

        public static class Builder {
            private WebhookDLQStats stats = new WebhookDLQStats();

            public Builder totalMessages(int totalMessages) {
                stats.totalMessages = totalMessages;
                return this;
            }

            public Builder affectedMerchants(int affectedMerchants) {
                stats.affectedMerchants = affectedMerchants;
                return this;
            }

            public Builder messagesLast24Hours(int messagesLast24Hours) {
                stats.messagesLast24Hours = messagesLast24Hours;
                return this;
            }

            public Builder oldestMessageTimestamp(Instant oldestMessageTimestamp) {
                stats.oldestMessageTimestamp = oldestMessageTimestamp;
                return this;
            }

            public WebhookDLQStats build() {
                return stats;
            }
        }
    }
}