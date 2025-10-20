package com.zendapag.api.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    public RateLimiter paymentsApiRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(100) // 100 requests per minute
                .timeoutDuration(Duration.ofSeconds(3))
                .build();

        return registry.rateLimiter("payments-api", config);
    }

    @Bean
    public RateLimiter merchantsApiRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(50) // 50 requests per minute
                .timeoutDuration(Duration.ofSeconds(3))
                .build();

        return registry.rateLimiter("merchants-api", config);
    }

    @Bean
    public RateLimiter webhooksApiRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(20) // 20 requests per minute
                .timeoutDuration(Duration.ofSeconds(3))
                .build();

        return registry.rateLimiter("webhooks-api", config);
    }

    @Bean
    public RateLimiter reportsApiRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(10) // 10 requests per minute (reports are expensive)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        return registry.rateLimiter("reports-api", config);
    }

    @Bean
    public RateLimiter authApiRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(30) // 30 auth requests per minute
                .timeoutDuration(Duration.ofSeconds(2))
                .build();

        return registry.rateLimiter("auth-api", config);
    }
}