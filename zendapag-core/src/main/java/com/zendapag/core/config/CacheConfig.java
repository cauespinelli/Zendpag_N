package com.zendapag.core.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Short-term cache for API responses (5 minutes)
        cacheConfigurations.put("api-responses", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(5)));

        // Medium-term cache for merchants (30 minutes)
        cacheConfigurations.put("merchants", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(30)));

        // Medium-term cache for customers (20 minutes)
        cacheConfigurations.put("customers", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(20)));

        // Short-term cache for payments (10 minutes)
        cacheConfigurations.put("payments", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(10)));

        // Short-term cache for transactions (5 minutes)
        cacheConfigurations.put("transactions", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(5)));

        // Medium-term cache for payment methods (25 minutes)
        cacheConfigurations.put("paymentMethods", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(25)));

        // Long-term cache for API keys (1 hour)
        cacheConfigurations.put("apiKeys", defaultCacheConfig
                .entryTtl(Duration.ofHours(1)));

        // Short-term cache for webhooks (5 minutes)
        cacheConfigurations.put("webhooks", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(5)));

        // Medium-term cache for disputes (15 minutes)
        cacheConfigurations.put("disputes", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(15)));

        // Medium-term cache for settlements (20 minutes)
        cacheConfigurations.put("settlements", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(20)));

        // Very short cache for audit logs (2 minutes)
        cacheConfigurations.put("auditLogs", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(2)));

        // Long-term cache for configuration data (2 hours)
        cacheConfigurations.put("configuration", defaultCacheConfig
                .entryTtl(Duration.ofHours(2)));

        // Medium-term cache for business rules (45 minutes)
        cacheConfigurations.put("businessRules", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(45)));

        // Short-term cache for rate limiting (1 minute)
        cacheConfigurations.put("rateLimiting", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(1)));

        // Long-term cache for reference data (4 hours)
        cacheConfigurations.put("referenceData", defaultCacheConfig
                .entryTtl(Duration.ofHours(4)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .withCacheConfiguration("merchants",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .computePrefixWith(cacheName -> "zendapag:cache:" + cacheName + ":"))
                .withCacheConfiguration("payments",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .computePrefixWith(cacheName -> "zendapag:cache:" + cacheName + ":"))
                .withCacheConfiguration("apiKeys",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1))
                                .computePrefixWith(cacheName -> "zendapag:cache:" + cacheName + ":"));
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // Enable transaction support
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new StringRedisSerializer());
        return template;
    }
}