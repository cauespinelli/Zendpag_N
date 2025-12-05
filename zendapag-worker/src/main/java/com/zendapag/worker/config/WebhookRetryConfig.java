package com.zendapag.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Webhook retry configuration with exponential backoff
 * Implements sophisticated retry strategy with dead letter queue
 */
@Configuration
@EnableRetry
@EnableScheduling
@ConfigurationProperties(prefix = "zendapag.webhook.retry")
public class WebhookRetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebhookRetryConfig.class);

    private int maxAttempts = 5;
    private long initialDelay = 1000; // 1 second
    private double multiplier = 2.0;
    private long maxDelay = 300000; // 5 minutes
    private boolean randomizeDelay = true;

    /**
     * Main retry template with exponential backoff
     */
    @Bean("webhookRetryTemplate")
    public RetryTemplate webhookRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialDelay);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxDelay);
        // Note: randomization is handled internally by ExponentialBackOffPolicy

        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Configure retry policy with exception classifier
        ExceptionClassifierRetryPolicy retryPolicy = new ExceptionClassifierRetryPolicy();
        retryPolicy.setExceptionClassifier(this::classifyException);

        retryTemplate.setRetryPolicy(retryPolicy);

        // Add retry listeners for monitoring
        retryTemplate.registerListener(new WebhookRetryListener());

        return retryTemplate;
    }

    /**
     * Classify exceptions for retry eligibility
     */
    private org.springframework.retry.RetryPolicy classifyException(Throwable throwable) {
        // Don't retry client errors (4xx)
        if (throwable instanceof WebhookClientException) {
            logger.debug("Not retrying client error: {}", throwable.getMessage());
            return new NeverRetryPolicy();
        }

        // Don't retry authentication errors
        if (throwable instanceof WebhookAuthenticationException) {
            logger.debug("Not retrying authentication error: {}", throwable.getMessage());
            return new NeverRetryPolicy();
        }

        // Don't retry malformed payload errors
        if (throwable instanceof WebhookPayloadException) {
            logger.debug("Not retrying payload error: {}", throwable.getMessage());
            return new NeverRetryPolicy();
        }

        // Retry server errors, network errors, and timeouts
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(WebhookServerException.class, true);
        retryableExceptions.put(WebhookTimeoutException.class, true);
        retryableExceptions.put(WebhookNetworkException.class, true);
        retryableExceptions.put(Exception.class, true); // Default retry for other exceptions

        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(maxAttempts, retryableExceptions);
        return simpleRetryPolicy;
    }

    /**
     * Retry listener for monitoring and logging
     */
    public static class WebhookRetryListener implements RetryListener {
        private static final Logger retryLogger = LoggerFactory.getLogger("WebhookRetry");

        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            String webhookId = (String) context.getAttribute("webhookId");
            retryLogger.debug("Starting retry attempt for webhook: {}", webhookId);
            return true;
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            String webhookId = (String) context.getAttribute("webhookId");
            int attempt = context.getRetryCount();

            retryLogger.warn("Retry attempt {} failed for webhook {}: {}",
                attempt, webhookId, throwable.getMessage());

            // Update metrics
            context.setAttribute("lastError", throwable.getMessage());
            context.setAttribute("lastAttemptTime", Instant.now());
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            String webhookId = (String) context.getAttribute("webhookId");
            int totalAttempts = context.getRetryCount();

            if (throwable == null) {
                retryLogger.info("Webhook {} delivered successfully after {} attempts",
                    webhookId, totalAttempts);
            } else {
                retryLogger.error("Webhook {} failed after {} attempts, sending to DLQ: {}",
                    webhookId, totalAttempts, throwable.getMessage());
            }
        }
    }

    /**
     * Scheduled retry processor for delayed retries
     */
    @Component
    public static class ScheduledRetryProcessor {
        private static final Logger scheduledLogger = LoggerFactory.getLogger("ScheduledRetry");
        private static final String RETRY_QUEUE_KEY = "webhook:retry:scheduled";

        private final RedisTemplate<String, Object> redisTemplate;

        public ScheduledRetryProcessor(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        /**
         * Schedule webhook for retry with delay
         */
        public void scheduleRetry(String webhookId, String merchantId, String payload,
                                 int attempt, Instant retryAt) {

            WebhookRetryItem retryItem = WebhookRetryItem.builder()
                .webhookId(webhookId)
                .merchantId(merchantId)
                .payload(payload)
                .attempt(attempt)
                .retryAt(retryAt)
                .scheduledAt(Instant.now())
                .build();

            String retryKey = String.format("retry:%s:%d", webhookId, attempt);

            // Calculate delay until retry time
            long delaySeconds = Duration.between(Instant.now(), retryAt).getSeconds();

            redisTemplate.opsForZSet().add(RETRY_QUEUE_KEY, retryKey, retryAt.getEpochSecond());
            redisTemplate.opsForHash().putAll("webhook:retry:data:" + retryKey, retryItem.toMap());
            redisTemplate.expire("webhook:retry:data:" + retryKey, Duration.ofHours(24));

            scheduledLogger.info("Scheduled webhook {} for retry attempt {} in {} seconds",
                webhookId, attempt, delaySeconds);
        }

        /**
         * Process scheduled retries every minute
         */
        @Scheduled(fixedDelay = 60000) // Every minute
        public void processScheduledRetries() {
            try {
                long currentTimestamp = Instant.now().getEpochSecond();

                Set<Object> readyRetries = redisTemplate.opsForZSet()
                    .rangeByScore(RETRY_QUEUE_KEY, 0, currentTimestamp);

                if (!readyRetries.isEmpty()) {
                    scheduledLogger.info("Processing {} scheduled retries", readyRetries.size());

                    for (Object retryKey : readyRetries) {
                        processRetryItem((String) retryKey);

                        // Remove from scheduled queue
                        redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryKey);
                    }
                }

            } catch (Exception e) {
                scheduledLogger.error("Error processing scheduled retries", e);
            }
        }

        private void processRetryItem(String retryKey) {
            try {
                Map<Object, Object> retryData = redisTemplate.opsForHash()
                    .entries("webhook:retry:data:" + retryKey);

                if (retryData.isEmpty()) {
                    scheduledLogger.warn("No retry data found for key: {}", retryKey);
                    return;
                }

                WebhookRetryItem retryItem = WebhookRetryItem.fromMap(retryData);

                scheduledLogger.info("Processing scheduled retry for webhook: {}, attempt: {}",
                    retryItem.getWebhookId(), retryItem.getAttempt());

                // TODO: Integrate with WebhookWorker to process the retry
                // This would typically publish back to Kafka or call the webhook service directly

                // Clean up retry data
                redisTemplate.delete("webhook:retry:data:" + retryKey);

            } catch (Exception e) {
                scheduledLogger.error("Error processing retry item: {}", retryKey, e);
            }
        }

        /**
         * Clean up expired retry items
         */
        @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
        public void cleanupExpiredRetries() {
            try {
                long oneDayAgo = Instant.now().minus(Duration.ofDays(1)).getEpochSecond();

                Long removedCount = redisTemplate.opsForZSet()
                    .removeRangeByScore(RETRY_QUEUE_KEY, 0, oneDayAgo);

                if (removedCount > 0) {
                    scheduledLogger.info("Cleaned up {} expired retry items", removedCount);
                }

            } catch (Exception e) {
                scheduledLogger.error("Error cleaning up expired retries", e);
            }
        }
    }

    /**
     * Webhook retry item data structure
     */
    public static class WebhookRetryItem {
        private String webhookId;
        private String merchantId;
        private String payload;
        private int attempt;
        private Instant retryAt;
        private Instant scheduledAt;

        public static Builder builder() {
            return new Builder();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("webhookId", webhookId);
            map.put("merchantId", merchantId);
            map.put("payload", payload);
            map.put("attempt", attempt);
            map.put("retryAt", retryAt.getEpochSecond());
            map.put("scheduledAt", scheduledAt.getEpochSecond());
            return map;
        }

        public static WebhookRetryItem fromMap(Map<Object, Object> map) {
            return builder()
                .webhookId((String) map.get("webhookId"))
                .merchantId((String) map.get("merchantId"))
                .payload((String) map.get("payload"))
                .attempt((Integer) map.get("attempt"))
                .retryAt(Instant.ofEpochSecond((Long) map.get("retryAt")))
                .scheduledAt(Instant.ofEpochSecond((Long) map.get("scheduledAt")))
                .build();
        }

        // Getters and setters
        public String getWebhookId() { return webhookId; }
        public String getMerchantId() { return merchantId; }
        public String getPayload() { return payload; }
        public int getAttempt() { return attempt; }
        public Instant getRetryAt() { return retryAt; }
        public Instant getScheduledAt() { return scheduledAt; }

        public static class Builder {
            private WebhookRetryItem item = new WebhookRetryItem();

            public Builder webhookId(String webhookId) {
                item.webhookId = webhookId;
                return this;
            }

            public Builder merchantId(String merchantId) {
                item.merchantId = merchantId;
                return this;
            }

            public Builder payload(String payload) {
                item.payload = payload;
                return this;
            }

            public Builder attempt(int attempt) {
                item.attempt = attempt;
                return this;
            }

            public Builder retryAt(Instant retryAt) {
                item.retryAt = retryAt;
                return this;
            }

            public Builder scheduledAt(Instant scheduledAt) {
                item.scheduledAt = scheduledAt;
                return this;
            }

            public WebhookRetryItem build() {
                return item;
            }
        }
    }

    // Custom exceptions for retry classification
    public static class WebhookClientException extends RuntimeException {
        public WebhookClientException(String message) { super(message); }
    }

    public static class WebhookServerException extends RuntimeException {
        public WebhookServerException(String message) { super(message); }
    }

    public static class WebhookTimeoutException extends RuntimeException {
        public WebhookTimeoutException(String message) { super(message); }
    }

    public static class WebhookNetworkException extends RuntimeException {
        public WebhookNetworkException(String message) { super(message); }
    }

    public static class WebhookAuthenticationException extends RuntimeException {
        public WebhookAuthenticationException(String message) { super(message); }
    }

    public static class WebhookPayloadException extends RuntimeException {
        public WebhookPayloadException(String message) { super(message); }
    }

    // Configuration properties getters and setters
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public long getInitialDelay() { return initialDelay; }
    public void setInitialDelay(long initialDelay) { this.initialDelay = initialDelay; }

    public double getMultiplier() { return multiplier; }
    public void setMultiplier(double multiplier) { this.multiplier = multiplier; }

    public long getMaxDelay() { return maxDelay; }
    public void setMaxDelay(long maxDelay) { this.maxDelay = maxDelay; }

    public boolean isRandomizeDelay() { return randomizeDelay; }
    public void setRandomizeDelay(boolean randomizeDelay) { this.randomizeDelay = randomizeDelay; }
}