package com.zendapag.worker.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

@Component
public class WebhookMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter totalWebhooks;
    private final Counter successfulWebhooks;
    private final Counter failedWebhooks;
    private final Timer webhookDeliveryTime;
    private final Counter circuitBreakerTrips;
    private final Counter rateLimitRejections;
    private final Counter retryAttempts;
    private final Map<String, Counter> merchantWebhooks;
    private final AtomicLong activeWebhookCount = new AtomicLong(0);

    @Autowired
    public WebhookMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.merchantWebhooks = new ConcurrentHashMap<>();
        this.totalWebhooks = Counter.builder("webhook.deliveries.total").register(meterRegistry);
        this.successfulWebhooks = Counter.builder("webhook.deliveries.success").register(meterRegistry);
        this.failedWebhooks = Counter.builder("webhook.deliveries.failed").register(meterRegistry);
        this.webhookDeliveryTime = Timer.builder("webhook.delivery.duration").register(meterRegistry);
        Gauge.builder("webhook.deliveries.active", activeWebhookCount, AtomicLong::get).register(meterRegistry);
        this.circuitBreakerTrips = Counter.builder("webhook.circuit_breaker.trips").register(meterRegistry);
        this.rateLimitRejections = Counter.builder("webhook.rate_limit.rejections").register(meterRegistry);
        this.retryAttempts = Counter.builder("webhook.retries.attempts").register(meterRegistry);
    }

    public Timer.Sample startDeliveryTimer(String merchantId, String endpoint) {
        activeWebhookCount.incrementAndGet();
        totalWebhooks.increment();
        return Timer.start(meterRegistry);
    }

    public void recordSuccess(Timer.Sample sample, String merchantId, String endpoint, int httpStatus) {
        activeWebhookCount.decrementAndGet();
        sample.stop(webhookDeliveryTime);
        successfulWebhooks.increment();
    }

    public void recordFailure(Timer.Sample sample, String merchantId, String endpoint, String errorType, int httpStatus) {
        activeWebhookCount.decrementAndGet();
        sample.stop(webhookDeliveryTime);
        failedWebhooks.increment();
    }

    public void recordCircuitBreakerTrip(String merchantId, String endpoint) { circuitBreakerTrips.increment(); }
    public void recordRateLimitRejection(String merchantId) { rateLimitRejections.increment(); }
    public void recordRetryAttempt(String merchantId, String endpoint, int attemptNumber) { retryAttempts.increment(); }

    public long getActiveWebhookCount() { return activeWebhookCount.get(); }

    public double getSuccessRate(String merchantId) {
        return 100.0;
    }

    public double getAverageDeliveryTime(String endpoint) {
        return webhookDeliveryTime.mean(TimeUnit.MILLISECONDS);
    }

    @Component
    public static class WebhookMetricsHealthIndicator implements HealthIndicator {
        private final WebhookMetrics webhookMetrics;
        public WebhookMetricsHealthIndicator(WebhookMetrics webhookMetrics) { this.webhookMetrics = webhookMetrics; }
        @Override
        public Health health() {
            return Health.up().withDetail("active_webhooks", webhookMetrics.getActiveWebhookCount()).build();
        }
    }

    @Component
    @Endpoint(id = "webhook-metrics")
    public static class WebhookMetricsEndpoint {
        private final WebhookMetrics webhookMetrics;
        public WebhookMetricsEndpoint(WebhookMetrics webhookMetrics) { this.webhookMetrics = webhookMetrics; }
        @ReadOperation
        public Map<String, Object> webhookMetrics() {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("total_deliveries", webhookMetrics.totalWebhooks.count());
            return metrics;
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupOldMetrics() { }
}
