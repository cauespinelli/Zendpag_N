package com.zendapag.worker.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration for webhook delivery per merchant
 * Implements distributed rate limiting using Redis and Bucket4j
 */
@Configuration
@ConfigurationProperties(prefix = "zendapag.webhook.rate-limit")
public class WebhookRateLimitConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebhookRateLimitConfig.class);

    // Default rate limiting configuration
    private int defaultRatePerMinute = 100;
    private int burstCapacity = 200;
    private Duration refillPeriod = Duration.ofMinutes(1);
    private Duration bucketExpiration = Duration.ofHours(1);

    @Value("${zendapag.webhook.rate-limit.redis-prefix:webhook:ratelimit}")
    private String redisPrefix;

    /**
     * ProxyManager for distributed rate limiting with Redis
     */
    @Bean
    public ProxyManager<String> proxyManager(JedisPool jedisPool) {
        return JedisBasedProxyManager.builderFor(jedisPool)
            .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(bucketExpiration))
            .build();
    }

    /**
     * Webhook rate limiter component
     */
    @Component
    public static class WebhookRateLimiter {
        private static final Logger limiterLogger = LoggerFactory.getLogger("WebhookRateLimiter");

        private final ProxyManager<String> proxyManager;
        private final RedisTemplate<String, Object> redisTemplate;
        private final MeterRegistry meterRegistry;
        private final String redisPrefix;

        // Merchant-specific configurations cache
        private final Map<String, MerchantRateConfig> merchantConfigs = new ConcurrentHashMap<>();

        // Default configuration
        private final int defaultRatePerMinute;
        private final int burstCapacity;
        private final Duration refillPeriod;

        public WebhookRateLimiter(ProxyManager<String> proxyManager,
                                 RedisTemplate<String, Object> redisTemplate,
                                 MeterRegistry meterRegistry,
                                 @Value("${zendapag.webhook.rate-limit.redis-prefix:webhook:ratelimit}") String redisPrefix,
                                 @Value("${zendapag.webhook.rate-limit.default-rate-per-minute:100}") int defaultRatePerMinute,
                                 @Value("${zendapag.webhook.rate-limit.burst-capacity:200}") int burstCapacity,
                                 @Value("${zendapag.webhook.rate-limit.refill-period:PT1M}") Duration refillPeriod) {
            this.proxyManager = proxyManager;
            this.redisTemplate = redisTemplate;
            this.meterRegistry = meterRegistry;
            this.redisPrefix = redisPrefix;
            this.defaultRatePerMinute = defaultRatePerMinute;
            this.burstCapacity = burstCapacity;
            this.refillPeriod = refillPeriod;
        }

        /**
         * Check if webhook delivery is allowed for merchant
         */
        public RateLimitResult isAllowed(String merchantId, int tokensRequested) {
            try {
                MerchantRateConfig config = getMerchantRateConfig(merchantId);
                String bucketKey = generateBucketKey(merchantId);

                // Get or create bucket for merchant
                Bucket bucket = proxyManager.builder()
                    .build(bucketKey, () -> createBucketConfiguration(config));

                // Try to consume tokens
                boolean allowed = bucket.tryConsume(tokensRequested);

                // Update metrics
                updateMetrics(merchantId, tokensRequested, allowed);

                // Log rate limiting decision
                if (!allowed) {
                    limiterLogger.warn("Rate limit exceeded for merchant: {}, requested: {}, available: {}",
                        merchantId, tokensRequested, bucket.getAvailableTokens());
                }

                return RateLimitResult.builder()
                    .allowed(allowed)
                    .merchantId(merchantId)
                    .tokensRequested(tokensRequested)
                    .availableTokens(bucket.getAvailableTokens())
                    .capacity(config.getBurstCapacity())
                    .refillRate(config.getRatePerMinute())
                    .resetTime(calculateResetTime(bucket))
                    .build();

            } catch (Exception e) {
                limiterLogger.error("Error checking rate limit for merchant: {}", merchantId, e);

                // Fail open - allow request if rate limiter fails
                return RateLimitResult.builder()
                    .allowed(true)
                    .merchantId(merchantId)
                    .tokensRequested(tokensRequested)
                    .availableTokens(Long.MAX_VALUE)
                    .capacity(defaultRatePerMinute)
                    .refillRate(defaultRatePerMinute)
                    .resetTime(Instant.now().plus(refillPeriod))
                    .error("Rate limiter error: " + e.getMessage())
                    .build();
            }
        }

        /**
         * Check if single webhook delivery is allowed
         */
        public RateLimitResult isAllowed(String merchantId) {
            return isAllowed(merchantId, 1);
        }

        /**
         * Get current rate limit status for merchant
         */
        public RateLimitStatus getStatus(String merchantId) {
            try {
                MerchantRateConfig config = getMerchantRateConfig(merchantId);
                String bucketKey = generateBucketKey(merchantId);

                Bucket bucket = proxyManager.builder()
                    .build(bucketKey, () -> createBucketConfiguration(config));

                return RateLimitStatus.builder()
                    .merchantId(merchantId)
                    .availableTokens(bucket.getAvailableTokens())
                    .capacity(config.getBurstCapacity())
                    .refillRate(config.getRatePerMinute())
                    .resetTime(calculateResetTime(bucket))
                    .lastRefill(Instant.now())
                    .build();

            } catch (Exception e) {
                limiterLogger.error("Error getting rate limit status for merchant: {}", merchantId, e);
                return null;
            }
        }

        /**
         * Configure custom rate limit for merchant
         */
        public void configureMerchant(String merchantId, int ratePerMinute, int burstCapacity) {
            MerchantRateConfig config = new MerchantRateConfig(merchantId, ratePerMinute, burstCapacity);

            // Cache configuration
            merchantConfigs.put(merchantId, config);

            // Store in Redis for persistence
            saveConfigToRedis(config);

            limiterLogger.info("Configured rate limit for merchant {}: {} req/min, burst: {}",
                merchantId, ratePerMinute, burstCapacity);
        }

        /**
         * Reset rate limit for merchant (emergency use)
         */
        public void resetMerchantRateLimit(String merchantId) {
            try {
                String bucketKey = generateBucketKey(merchantId);

                // Remove bucket from distributed cache
                proxyManager.removeProxy(bucketKey);

                limiterLogger.info("Reset rate limit for merchant: {}", merchantId);

            } catch (Exception e) {
                limiterLogger.error("Error resetting rate limit for merchant: {}", merchantId, e);
            }
        }

        /**
         * Get merchant rate configuration
         */
        private MerchantRateConfig getMerchantRateConfig(String merchantId) {
            return merchantConfigs.computeIfAbsent(merchantId, this::loadConfigFromRedis);
        }

        /**
         * Load merchant configuration from Redis
         */
        private MerchantRateConfig loadConfigFromRedis(String merchantId) {
            try {
                String configKey = redisPrefix + ":config:" + merchantId;
                Map<Object, Object> configData = redisTemplate.opsForHash().entries(configKey);

                if (!configData.isEmpty()) {
                    int ratePerMinute = (Integer) configData.getOrDefault("ratePerMinute", defaultRatePerMinute);
                    int burstCapacity = (Integer) configData.getOrDefault("burstCapacity", this.burstCapacity);

                    return new MerchantRateConfig(merchantId, ratePerMinute, burstCapacity);
                }
            } catch (Exception e) {
                limiterLogger.warn("Error loading config for merchant {}, using defaults", merchantId, e);
            }

            // Return default configuration
            return new MerchantRateConfig(merchantId, defaultRatePerMinute, burstCapacity);
        }

        /**
         * Save merchant configuration to Redis
         */
        private void saveConfigToRedis(MerchantRateConfig config) {
            try {
                String configKey = redisPrefix + ":config:" + config.getMerchantId();

                Map<String, Object> configData = Map.of(
                    "merchantId", config.getMerchantId(),
                    "ratePerMinute", config.getRatePerMinute(),
                    "burstCapacity", config.getBurstCapacity(),
                    "updatedAt", Instant.now().getEpochSecond()
                );

                redisTemplate.opsForHash().putAll(configKey, configData);
                redisTemplate.expire(configKey, Duration.ofDays(30));

            } catch (Exception e) {
                limiterLogger.error("Error saving config for merchant: {}", config.getMerchantId(), e);
            }
        }

        /**
         * Create bucket configuration for merchant
         */
        private BucketConfiguration createBucketConfiguration(MerchantRateConfig config) {
            Bandwidth bandwidth = Bandwidth.builder()
                .capacity(config.getBurstCapacity())
                .refillGreedy(config.getRatePerMinute(), refillPeriod)
                .build();

            return BucketConfiguration.builder()
                .addLimit(bandwidth)
                .build();
        }

        /**
         * Generate unique bucket key for merchant
         */
        private String generateBucketKey(String merchantId) {
            return redisPrefix + ":bucket:" + merchantId;
        }

        /**
         * Calculate next reset time for bucket
         */
        private Instant calculateResetTime(Bucket bucket) {
            // This is an approximation since Bucket4j doesn't provide exact reset time
            long availableTokens = bucket.getAvailableTokens();
            long missingTokens = burstCapacity - availableTokens;

            if (missingTokens <= 0) {
                return Instant.now();
            }

            // Estimate time for bucket to refill
            long secondsToRefill = (missingTokens * 60) / defaultRatePerMinute;
            return Instant.now().plusSeconds(secondsToRefill);
        }

        /**
         * Update rate limiting metrics
         */
        private void updateMetrics(String merchantId, int tokensRequested, boolean allowed) {
            meterRegistry.counter("webhook.rate_limit.requests",
                "merchant", merchantId,
                "allowed", String.valueOf(allowed)
            ).increment();

            if (!allowed) {
                meterRegistry.counter("webhook.rate_limit.rejections",
                    "merchant", merchantId
                ).increment();
            }

            meterRegistry.gauge("webhook.rate_limit.tokens_requested",
                tokensRequested);
        }
    }

    /**
     * Merchant rate configuration
     */
    public static class MerchantRateConfig {
        private final String merchantId;
        private final int ratePerMinute;
        private final int burstCapacity;

        public MerchantRateConfig(String merchantId, int ratePerMinute, int burstCapacity) {
            this.merchantId = merchantId;
            this.ratePerMinute = ratePerMinute;
            this.burstCapacity = burstCapacity;
        }

        public String getMerchantId() { return merchantId; }
        public int getRatePerMinute() { return ratePerMinute; }
        public int getBurstCapacity() { return burstCapacity; }
    }

    /**
     * Rate limit result
     */
    public static class RateLimitResult {
        private boolean allowed;
        private String merchantId;
        private int tokensRequested;
        private long availableTokens;
        private int capacity;
        private int refillRate;
        private Instant resetTime;
        private String error;

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public boolean isAllowed() { return allowed; }
        public String getMerchantId() { return merchantId; }
        public int getTokensRequested() { return tokensRequested; }
        public long getAvailableTokens() { return availableTokens; }
        public int getCapacity() { return capacity; }
        public int getRefillRate() { return refillRate; }
        public Instant getResetTime() { return resetTime; }
        public String getError() { return error; }

        public static class Builder {
            private RateLimitResult result = new RateLimitResult();

            public Builder allowed(boolean allowed) {
                result.allowed = allowed;
                return this;
            }

            public Builder merchantId(String merchantId) {
                result.merchantId = merchantId;
                return this;
            }

            public Builder tokensRequested(int tokensRequested) {
                result.tokensRequested = tokensRequested;
                return this;
            }

            public Builder availableTokens(long availableTokens) {
                result.availableTokens = availableTokens;
                return this;
            }

            public Builder capacity(int capacity) {
                result.capacity = capacity;
                return this;
            }

            public Builder refillRate(int refillRate) {
                result.refillRate = refillRate;
                return this;
            }

            public Builder resetTime(Instant resetTime) {
                result.resetTime = resetTime;
                return this;
            }

            public Builder error(String error) {
                result.error = error;
                return this;
            }

            public RateLimitResult build() {
                return result;
            }
        }
    }

    /**
     * Rate limit status
     */
    public static class RateLimitStatus {
        private String merchantId;
        private long availableTokens;
        private int capacity;
        private int refillRate;
        private Instant resetTime;
        private Instant lastRefill;

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getMerchantId() { return merchantId; }
        public long getAvailableTokens() { return availableTokens; }
        public int getCapacity() { return capacity; }
        public int getRefillRate() { return refillRate; }
        public Instant getResetTime() { return resetTime; }
        public Instant getLastRefill() { return lastRefill; }

        public static class Builder {
            private RateLimitStatus status = new RateLimitStatus();

            public Builder merchantId(String merchantId) {
                status.merchantId = merchantId;
                return this;
            }

            public Builder availableTokens(long availableTokens) {
                status.availableTokens = availableTokens;
                return this;
            }

            public Builder capacity(int capacity) {
                status.capacity = capacity;
                return this;
            }

            public Builder refillRate(int refillRate) {
                status.refillRate = refillRate;
                return this;
            }

            public Builder resetTime(Instant resetTime) {
                status.resetTime = resetTime;
                return this;
            }

            public Builder lastRefill(Instant lastRefill) {
                status.lastRefill = lastRefill;
                return this;
            }

            public RateLimitStatus build() {
                return status;
            }
        }
    }

    // Configuration properties getters and setters
    public int getDefaultRatePerMinute() { return defaultRatePerMinute; }
    public void setDefaultRatePerMinute(int defaultRatePerMinute) { this.defaultRatePerMinute = defaultRatePerMinute; }

    public int getBurstCapacity() { return burstCapacity; }
    public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }

    public Duration getRefillPeriod() { return refillPeriod; }
    public void setRefillPeriod(Duration refillPeriod) { this.refillPeriod = refillPeriod; }

    public Duration getBucketExpiration() { return bucketExpiration; }
    public void setBucketExpiration(Duration bucketExpiration) { this.bucketExpiration = bucketExpiration; }
}