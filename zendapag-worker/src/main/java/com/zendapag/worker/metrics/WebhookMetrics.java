package com.zendapag.worker.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Comprehensive webhook metrics collection and monitoring
 * Provides detailed metrics for webhook delivery performance and reliability
 */
@Component
public class WebhookMetrics {

    private final MeterRegistry meterRegistry;

    // Core metrics
    private final Counter totalWebhooks;
    private final Counter successfulWebhooks;
    private final Counter failedWebhooks;
    private final Timer webhookDeliveryTime;
    private final Gauge activeWebhooks;

    // Circuit breaker metrics
    private final Counter circuitBreakerTrips;
    private final Counter rateLimitRejections;
    private final Counter retryAttempts;

    // Business metrics
    private final Map<String, Counter> merchantWebhooks;
    private final Map<String, Timer> endpointDeliveryTimes;

    // Runtime state
    private final AtomicLong activeWebhookCount = new AtomicLong(0);
    private final Map<String, WebhookEndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();

    @Autowired
    public WebhookMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.merchantWebhooks = new ConcurrentHashMap<>();
        this.endpointDeliveryTimes = new ConcurrentHashMap<>();

        // Initialize core metrics
        this.totalWebhooks = Counter.builder("webhook.deliveries.total")
            .description("Total number of webhook delivery attempts")
            .register(meterRegistry);

        this.successfulWebhooks = Counter.builder("webhook.deliveries.success")
            .description("Number of successful webhook deliveries")
            .register(meterRegistry);

        this.failedWebhooks = Counter.builder("webhook.deliveries.failed")
            .description("Number of failed webhook deliveries")
            .register(meterRegistry);

        this.webhookDeliveryTime = Timer.builder("webhook.delivery.duration")
            .description("Time taken for webhook delivery")
            .register(meterRegistry);

        this.activeWebhooks = Gauge.builder("webhook.deliveries.active")
            .description("Number of currently active webhook deliveries")
            .register(meterRegistry, this, WebhookMetrics::getActiveWebhookCount);

        this.circuitBreakerTrips = Counter.builder("webhook.circuit_breaker.trips")
            .description("Number of circuit breaker trips")
            .register(meterRegistry);

        this.rateLimitRejections = Counter.builder("webhook.rate_limit.rejections")
            .description("Number of rate limit rejections")
            .register(meterRegistry);

        this.retryAttempts = Counter.builder("webhook.retries.attempts")
            .description("Number of retry attempts")
            .register(meterRegistry);
    }

    /**
     * Record webhook delivery attempt start
     */
    public Timer.Sample startDeliveryTimer(String merchantId, String endpoint) {
        activeWebhookCount.incrementAndGet();
        totalWebhooks.increment(Tags.of("merchant", merchantId, "endpoint", maskEndpoint(endpoint)));

        // Update endpoint-specific metrics
        updateEndpointMetrics(endpoint, "attempt", 1);

        return Timer.start(meterRegistry);
    }

    /**
     * Record successful webhook delivery
     */
    public void recordSuccess(Timer.Sample sample, String merchantId, String endpoint, int httpStatus) {
        activeWebhookCount.decrementAndGet();

        // Stop timer and record delivery time
        Duration deliveryTime = sample.stop(webhookDeliveryTime.tag("merchant", merchantId)
            .tag("endpoint", maskEndpoint(endpoint))
            .tag("status", "success")
            .tag("http_status", String.valueOf(httpStatus)));

        successfulWebhooks.increment(Tags.of("merchant", merchantId, "endpoint", maskEndpoint(endpoint)));

        // Update merchant-specific counter
        getMerchantCounter(merchantId, "success").increment();

        // Update endpoint-specific timer
        getEndpointTimer(endpoint).record(deliveryTime);

        // Update endpoint metrics
        updateEndpointMetrics(endpoint, "success", 1);
        updateEndpointMetrics(endpoint, "delivery_time", deliveryTime.toMillis());
    }

    /**
     * Record failed webhook delivery
     */
    public void recordFailure(Timer.Sample sample, String merchantId, String endpoint, String errorType, int httpStatus) {
        activeWebhookCount.decrementAndGet();

        // Stop timer
        Duration deliveryTime = sample.stop(webhookDeliveryTime.tag("merchant", merchantId)
            .tag("endpoint", maskEndpoint(endpoint))
            .tag("status", "failed")
            .tag("error_type", errorType)
            .tag("http_status", String.valueOf(httpStatus)));

        failedWebhooks.increment(Tags.of("merchant", merchantId, "endpoint", maskEndpoint(endpoint), "error_type", errorType));

        // Update merchant-specific counter
        getMerchantCounter(merchantId, "failed").increment();

        // Update endpoint metrics
        updateEndpointMetrics(endpoint, "failure", 1);
        updateEndpointMetrics(endpoint, "delivery_time", deliveryTime.toMillis());
    }

    /**
     * Record circuit breaker trip
     */
    public void recordCircuitBreakerTrip(String merchantId, String endpoint) {
        circuitBreakerTrips.increment(Tags.of("merchant", merchantId, "endpoint", maskEndpoint(endpoint)));
        updateEndpointMetrics(endpoint, "circuit_breaker_trip", 1);
    }

    /**
     * Record rate limit rejection
     */
    public void recordRateLimitRejection(String merchantId) {
        rateLimitRejections.increment(Tags.of("merchant", merchantId));
    }

    /**
     * Record retry attempt
     */
    public void recordRetryAttempt(String merchantId, String endpoint, int attemptNumber) {
        retryAttempts.increment(Tags.of(
            "merchant", merchantId,
            "endpoint", maskEndpoint(endpoint),
            "attempt", String.valueOf(attemptNumber)
        ));

        updateEndpointMetrics(endpoint, "retry_attempt", 1);
    }

    /**
     * Get merchant-specific counter
     */
    private Counter getMerchantCounter(String merchantId, String type) {
        String key = merchantId + ":" + type;
        return merchantWebhooks.computeIfAbsent(key, k ->
            Counter.builder("webhook.deliveries.by_merchant")
                .description("Webhook deliveries by merchant")
                .tag("merchant", merchantId)
                .tag("type", type)
                .register(meterRegistry)
        );
    }

    /**
     * Get endpoint-specific timer
     */
    private Timer getEndpointTimer(String endpoint) {
        return endpointDeliveryTimes.computeIfAbsent(endpoint, ep ->
            Timer.builder("webhook.delivery.duration.by_endpoint")
                .description("Webhook delivery time by endpoint")
                .tag("endpoint", maskEndpoint(ep))
                .register(meterRegistry)
        );
    }

    /**
     * Update endpoint-specific metrics
     */
    private void updateEndpointMetrics(String endpoint, String metricType, double value) {
        endpointMetrics.compute(endpoint, (key, metrics) -> {
            if (metrics == null) {
                metrics = new WebhookEndpointMetrics(endpoint);
            }
            metrics.updateMetric(metricType, value);
            return metrics;
        });
    }

    /**
     * Get active webhook count
     */
    public long getActiveWebhookCount() {
        return activeWebhookCount.get();
    }

    /**
     * Calculate success rate for merchant
     */
    public double getSuccessRate(String merchantId) {
        double successful = getMerchantCounter(merchantId, "success").count();
        double failed = getMerchantCounter(merchantId, "failed").count();
        double total = successful + failed;

        return total > 0 ? (successful / total) * 100 : 0;
    }

    /**
     * Calculate average delivery time for endpoint
     */
    public double getAverageDeliveryTime(String endpoint) {
        Timer timer = endpointDeliveryTimes.get(endpoint);
        return timer != null ? timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS) : 0;
    }

    /**
     * Mask endpoint URL for privacy
     */
    private String maskEndpoint(String endpoint) {
        try {
            java.net.URL url = new java.net.URL(endpoint);
            return url.getHost() + ":***";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Webhook metrics health indicator
     */
    @Component
    public static class WebhookMetricsHealthIndicator implements HealthIndicator {

        private final WebhookMetrics webhookMetrics;
        private final MeterRegistry meterRegistry;

        private static final double CRITICAL_SUCCESS_RATE_THRESHOLD = 90.0;
        private static final double WARNING_SUCCESS_RATE_THRESHOLD = 95.0;
        private static final long CRITICAL_ACTIVE_WEBHOOKS_THRESHOLD = 1000;

        public WebhookMetricsHealthIndicator(WebhookMetrics webhookMetrics, MeterRegistry meterRegistry) {
            this.webhookMetrics = webhookMetrics;
            this.meterRegistry = meterRegistry;
        }

        @Override
        public Health health() {
            try {
                // Calculate overall metrics
                double overallSuccessRate = calculateOverallSuccessRate();
                long activeWebhooks = webhookMetrics.getActiveWebhookCount();
                double avgDeliveryTime = calculateAverageDeliveryTime();

                Health.Builder healthBuilder = Health.up();

                // Check success rate
                if (overallSuccessRate < CRITICAL_SUCCESS_RATE_THRESHOLD) {
                    healthBuilder = Health.down()
                        .withDetail("issue", "Critical webhook success rate");
                } else if (overallSuccessRate < WARNING_SUCCESS_RATE_THRESHOLD) {
                    healthBuilder = Health.status(Status.UNKNOWN)
                        .withDetail("issue", "Low webhook success rate");
                }

                // Check active webhooks
                if (activeWebhooks > CRITICAL_ACTIVE_WEBHOOKS_THRESHOLD) {
                    healthBuilder = Health.down()
                        .withDetail("issue", "Too many active webhooks");
                }

                return healthBuilder
                    .withDetail("success_rate", String.format("%.2f%%", overallSuccessRate))
                    .withDetail("active_webhooks", activeWebhooks)
                    .withDetail("avg_delivery_time_ms", String.format("%.2f", avgDeliveryTime))
                    .withDetail("total_deliveries", webhookMetrics.totalWebhooks.count())
                    .withDetail("successful_deliveries", webhookMetrics.successfulWebhooks.count())
                    .withDetail("failed_deliveries", webhookMetrics.failedWebhooks.count())
                    .build();

            } catch (Exception e) {
                return Health.down()
                    .withDetail("error", "Error collecting webhook metrics: " + e.getMessage())
                    .build();
            }
        }

        private double calculateOverallSuccessRate() {
            double successful = webhookMetrics.successfulWebhooks.count();
            double failed = webhookMetrics.failedWebhooks.count();
            double total = successful + failed;

            return total > 0 ? (successful / total) * 100 : 100;
        }

        private double calculateAverageDeliveryTime() {
            HistogramSnapshot snapshot = webhookMetrics.webhookDeliveryTime.takeSnapshot();
            return snapshot.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Webhook metrics endpoint
     */
    @Component
    @Endpoint(id = "webhook-metrics")
    public static class WebhookMetricsEndpoint {

        private final WebhookMetrics webhookMetrics;
        private final MeterRegistry meterRegistry;

        public WebhookMetricsEndpoint(WebhookMetrics webhookMetrics, MeterRegistry meterRegistry) {
            this.webhookMetrics = webhookMetrics;
            this.meterRegistry = meterRegistry;
        }

        @ReadOperation
        public Map<String, Object> webhookMetrics() {
            Map<String, Object> metrics = new HashMap<>();

            // Overall metrics
            metrics.put("total_deliveries", webhookMetrics.totalWebhooks.count());
            metrics.put("successful_deliveries", webhookMetrics.successfulWebhooks.count());
            metrics.put("failed_deliveries", webhookMetrics.failedWebhooks.count());
            metrics.put("active_deliveries", webhookMetrics.getActiveWebhookCount());

            // Success rate
            double successful = webhookMetrics.successfulWebhooks.count();
            double failed = webhookMetrics.failedWebhooks.count();
            double total = successful + failed;
            double successRate = total > 0 ? (successful / total) * 100 : 100;
            metrics.put("success_rate_percentage", String.format("%.2f", successRate));

            // Delivery time statistics
            HistogramSnapshot deliveryTimeSnapshot = webhookMetrics.webhookDeliveryTime.takeSnapshot();
            Map<String, Object> deliveryTimes = new HashMap<>();
            deliveryTimes.put("mean_ms", deliveryTimeSnapshot.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
            deliveryTimes.put("max_ms", deliveryTimeSnapshot.max(java.util.concurrent.TimeUnit.MILLISECONDS));
            deliveryTimes.put("p50_ms", deliveryTimeSnapshot.percentileValue(0.5, java.util.concurrent.TimeUnit.MILLISECONDS));
            deliveryTimes.put("p95_ms", deliveryTimeSnapshot.percentileValue(0.95, java.util.concurrent.TimeUnit.MILLISECONDS));
            deliveryTimes.put("p99_ms", deliveryTimeSnapshot.percentileValue(0.99, java.util.concurrent.TimeUnit.MILLISECONDS));
            metrics.put("delivery_times", deliveryTimes);

            // Error metrics
            metrics.put("circuit_breaker_trips", webhookMetrics.circuitBreakerTrips.count());
            metrics.put("rate_limit_rejections", webhookMetrics.rateLimitRejections.count());
            metrics.put("retry_attempts", webhookMetrics.retryAttempts.count());

            // Endpoint metrics
            Map<String, Object> endpointMetrics = new HashMap<>();
            webhookMetrics.endpointMetrics.forEach((endpoint, metrics_data) -> {
                endpointMetrics.put(webhookMetrics.maskEndpoint(endpoint), metrics_data.toMap());
            });
            metrics.put("endpoint_metrics", endpointMetrics);

            return metrics;
        }
    }

    /**
     * Endpoint-specific metrics storage
     */
    private static class WebhookEndpointMetrics {
        private final String endpoint;
        private final Map<String, DoubleAdder> metrics = new ConcurrentHashMap<>();
        private final Instant createdAt = Instant.now();

        public WebhookEndpointMetrics(String endpoint) {
            this.endpoint = endpoint;
        }

        public void updateMetric(String metricType, double value) {
            metrics.computeIfAbsent(metricType, k -> new DoubleAdder()).add(value);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("endpoint", endpoint);
            map.put("created_at", createdAt.toString());

            Map<String, Double> metricValues = new HashMap<>();
            metrics.forEach((key, value) -> metricValues.put(key, value.sum()));
            map.put("metrics", metricValues);

            return map;
        }
    }

    /**
     * Scheduled cleanup of old endpoint metrics
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour
    public void cleanupOldMetrics() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));

        endpointMetrics.entrySet().removeIf(entry ->
            entry.getValue().createdAt.isBefore(cutoff));
    }
}