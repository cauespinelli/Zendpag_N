package com.zendapag.worker.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit breaker configuration for webhook endpoints
 * Implements per-endpoint circuit breakers with Redis-based state sharing
 */
@Configuration
@ConfigurationProperties(prefix = "zendapag.webhook.circuit-breaker")
public class WebhookCircuitBreakerConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebhookCircuitBreakerConfig.class);

    // Configuration properties
    private int failureRateThreshold = 50;
    private int slowCallRateThreshold = 80;
    private Duration slowCallDurationThreshold = Duration.ofSeconds(5);
    private int minimumNumberOfCalls = 10;
    private int permittedNumberOfCallsInHalfOpenState = 5;
    private Duration waitDurationInOpenState = Duration.ofMinutes(1);
    private int slidingWindowSize = 100;
    private String slidingWindowType = "COUNT_BASED";
    private boolean automaticTransitionFromOpenToHalfOpenEnabled = true;

    @Value("${zendapag.webhook.circuit-breaker.redis-prefix:webhook:cb}")
    private String redisPrefix;

    /**
     * Circuit breaker registry with custom configuration
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .slowCallRateThreshold(slowCallRateThreshold)
            .slowCallDurationThreshold(slowCallDurationThreshold)
            .minimumNumberOfCalls(minimumNumberOfCalls)
            .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
            .waitDurationInOpenState(waitDurationInOpenState)
            .slidingWindowSize(slidingWindowSize)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(slidingWindowType))
            .automaticTransitionFromOpenToHalfOpenEnabled(automaticTransitionFromOpenToHalfOpenEnabled)
            .recordExceptions(
                WebhookRetryConfig.WebhookServerException.class,
                WebhookRetryConfig.WebhookTimeoutException.class,
                WebhookRetryConfig.WebhookNetworkException.class
            )
            .ignoreExceptions(
                WebhookRetryConfig.WebhookClientException.class,
                WebhookRetryConfig.WebhookAuthenticationException.class,
                WebhookRetryConfig.WebhookPayloadException.class
            )
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Register event consumer for monitoring
        registry.getEventPublisher()
            .onEntryAdded(event -> logger.info("Circuit breaker added: {}", event.getAddedEntry().getName()))
            .onEntryRemoved(event -> logger.info("Circuit breaker removed: {}", event.getRemovedEntry().getName()))
            .onEntryReplaced(event -> logger.info("Circuit breaker replaced: {}", event.getNewEntry().getName()));

        return registry;
    }

    /**
     * Circuit breaker manager for webhook endpoints
     */
    @Component
    public static class WebhookCircuitBreakerManager {
        private static final Logger managerLogger = LoggerFactory.getLogger("CircuitBreakerManager");

        private final CircuitBreakerRegistry registry;
        private final RedisTemplate<String, Object> redisTemplate;
        private final MeterRegistry meterRegistry;
        private final Map<String, CircuitBreaker> endpointCircuitBreakers = new ConcurrentHashMap<>();
        private final String redisPrefix;

        public WebhookCircuitBreakerManager(CircuitBreakerRegistry registry,
                                          RedisTemplate<String, Object> redisTemplate,
                                          MeterRegistry meterRegistry,
                                          @Value("${zendapag.webhook.circuit-breaker.redis-prefix:webhook:cb}") String redisPrefix) {
            this.registry = registry;
            this.redisTemplate = redisTemplate;
            this.meterRegistry = meterRegistry;
            this.redisPrefix = redisPrefix;
        }

        /**
         * Get or create circuit breaker for endpoint
         */
        public CircuitBreaker getCircuitBreaker(String webhookUrl, String merchantId) {
            String endpointKey = generateEndpointKey(webhookUrl, merchantId);

            return endpointCircuitBreakers.computeIfAbsent(endpointKey, key -> {
                CircuitBreaker circuitBreaker = registry.circuitBreaker(key);

                // Add event listener for state changes
                circuitBreaker.getEventPublisher()
                    .onStateTransition(this::handleStateTransition)
                    .onCallNotPermitted(this::handleCallNotPermitted)
                    .onError(this::handleError)
                    .onSuccess(this::handleSuccess);

                managerLogger.info("Created circuit breaker for endpoint: {} (merchant: {})",
                    maskUrl(webhookUrl), merchantId);

                return circuitBreaker;
            });
        }

        /**
         * Generate unique key for endpoint
         */
        private String generateEndpointKey(String webhookUrl, String merchantId) {
            try {
                URL url = new URL(webhookUrl);
                String host = url.getHost();
                int port = url.getPort() == -1 ? (url.getProtocol().equals("https") ? 443 : 80) : url.getPort();
                return String.format("%s:%s:%d", merchantId, host, port);
            } catch (MalformedURLException e) {
                managerLogger.warn("Invalid webhook URL: {}, using fallback key", webhookUrl);
                return String.format("%s:%s", merchantId, webhookUrl.hashCode());
            }
        }

        /**
         * Check if circuit breaker allows call
         */
        public boolean isCallPermitted(String webhookUrl, String merchantId) {
            CircuitBreaker circuitBreaker = getCircuitBreaker(webhookUrl, merchantId);
            boolean permitted = circuitBreaker.tryAcquirePermission();

            if (!permitted) {
                managerLogger.debug("Circuit breaker OPEN - call not permitted for endpoint: {} (merchant: {})",
                    maskUrl(webhookUrl), merchantId);

                // Update Redis state
                updateRedisState(generateEndpointKey(webhookUrl, merchantId), "OPEN", Instant.now());
            }

            return permitted;
        }

        /**
         * Record successful call
         */
        public void recordSuccess(String webhookUrl, String merchantId, Duration duration) {
            CircuitBreaker circuitBreaker = getCircuitBreaker(webhookUrl, merchantId);
            circuitBreaker.onSuccess(duration.toMillis(), duration.toMillis() > 5000 ?
                java.util.concurrent.TimeUnit.MILLISECONDS : null);
        }

        /**
         * Record failed call
         */
        public void recordError(String webhookUrl, String merchantId, Duration duration, Throwable throwable) {
            CircuitBreaker circuitBreaker = getCircuitBreaker(webhookUrl, merchantId);
            circuitBreaker.onError(duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS, throwable);
        }

        /**
         * Get circuit breaker state
         */
        public CircuitBreakerState getCircuitBreakerState(String webhookUrl, String merchantId) {
            CircuitBreaker circuitBreaker = getCircuitBreaker(webhookUrl, merchantId);
            CircuitBreaker.State state = circuitBreaker.getState();

            return CircuitBreakerState.builder()
                .endpointKey(generateEndpointKey(webhookUrl, merchantId))
                .state(state.name())
                .failureRate(circuitBreaker.getMetrics().getFailureRate())
                .slowCallRate(circuitBreaker.getMetrics().getSlowCallRate())
                .numberOfCalls(circuitBreaker.getMetrics().getNumberOfCalls())
                .numberOfFailedCalls(circuitBreaker.getMetrics().getNumberOfFailedCalls())
                .numberOfSlowCalls(circuitBreaker.getMetrics().getNumberOfSlowCalls())
                .numberOfNotPermittedCalls(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls())
                .lastStateTransition(Instant.now())
                .build();
        }

        /**
         * Handle state transition events
         */
        private void handleStateTransition(CircuitBreakerEvent.StateTransitionEvent event) {
            managerLogger.info("Circuit breaker state transition: {} -> {} for: {}",
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState(),
                event.getCircuitBreakerName());

            // Update metrics
            meterRegistry.counter("webhook.circuit_breaker.state_transitions",
                "from", event.getStateTransition().getFromState().name(),
                "to", event.getStateTransition().getToState().name(),
                "endpoint", event.getCircuitBreakerName()
            ).increment();

            // Update Redis state
            updateRedisState(event.getCircuitBreakerName(),
                event.getStateTransition().getToState().name(),
                Instant.now());
        }

        /**
         * Handle call not permitted events
         */
        private void handleCallNotPermitted(CircuitBreakerEvent.CallNotPermittedEvent event) {
            managerLogger.debug("Call not permitted by circuit breaker: {}", event.getCircuitBreakerName());

            meterRegistry.counter("webhook.circuit_breaker.calls_not_permitted",
                "endpoint", event.getCircuitBreakerName()
            ).increment();
        }

        /**
         * Handle error events
         */
        private void handleError(CircuitBreakerEvent.ErrorEvent event) {
            managerLogger.debug("Circuit breaker recorded error for {}: {}",
                event.getCircuitBreakerName(), event.getThrowable().getMessage());

            meterRegistry.counter("webhook.circuit_breaker.errors",
                "endpoint", event.getCircuitBreakerName(),
                "error_type", event.getThrowable().getClass().getSimpleName()
            ).increment();
        }

        /**
         * Handle success events
         */
        private void handleSuccess(CircuitBreakerEvent.SuccessEvent event) {
            managerLogger.debug("Circuit breaker recorded success for {} (duration: {}ms)",
                event.getCircuitBreakerName(), event.getElapsedDuration().toMillis());

            meterRegistry.timer("webhook.circuit_breaker.success_duration",
                "endpoint", event.getCircuitBreakerName()
            ).record(event.getElapsedDuration());
        }

        /**
         * Update circuit breaker state in Redis
         */
        private void updateRedisState(String endpointKey, String state, Instant timestamp) {
            try {
                String redisKey = redisPrefix + ":" + endpointKey;

                Map<String, Object> stateData = Map.of(
                    "state", state,
                    "timestamp", timestamp.getEpochSecond(),
                    "updated_at", Instant.now().getEpochSecond()
                );

                redisTemplate.opsForHash().putAll(redisKey, stateData);
                redisTemplate.expire(redisKey, Duration.ofHours(24));

            } catch (Exception e) {
                managerLogger.error("Error updating circuit breaker state in Redis", e);
            }
        }

        /**
         * Mask URL for logging security
         */
        private String maskUrl(String url) {
            try {
                URL parsedUrl = new URL(url);
                return parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + ":***";
            } catch (MalformedURLException e) {
                return "***masked***";
            }
        }

        /**
         * Get all circuit breaker states
         */
        public Map<String, CircuitBreakerState> getAllCircuitBreakerStates() {
            Map<String, CircuitBreakerState> states = new ConcurrentHashMap<>();

            endpointCircuitBreakers.forEach((key, circuitBreaker) -> {
                CircuitBreaker.State state = circuitBreaker.getState();

                CircuitBreakerState cbState = CircuitBreakerState.builder()
                    .endpointKey(key)
                    .state(state.name())
                    .failureRate(circuitBreaker.getMetrics().getFailureRate())
                    .slowCallRate(circuitBreaker.getMetrics().getSlowCallRate())
                    .numberOfCalls(circuitBreaker.getMetrics().getNumberOfCalls())
                    .numberOfFailedCalls(circuitBreaker.getMetrics().getNumberOfFailedCalls())
                    .numberOfSlowCalls(circuitBreaker.getMetrics().getNumberOfSlowCalls())
                    .numberOfNotPermittedCalls(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls())
                    .lastStateTransition(Instant.now())
                    .build();

                states.put(key, cbState);
            });

            return states;
        }
    }

    /**
     * Circuit breaker state data class
     */
    public static class CircuitBreakerState {
        private String endpointKey;
        private String state;
        private float failureRate;
        private float slowCallRate;
        private int numberOfCalls;
        private int numberOfFailedCalls;
        private int numberOfSlowCalls;
        private long numberOfNotPermittedCalls;
        private Instant lastStateTransition;

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getEndpointKey() { return endpointKey; }
        public String getState() { return state; }
        public float getFailureRate() { return failureRate; }
        public float getSlowCallRate() { return slowCallRate; }
        public int getNumberOfCalls() { return numberOfCalls; }
        public int getNumberOfFailedCalls() { return numberOfFailedCalls; }
        public int getNumberOfSlowCalls() { return numberOfSlowCalls; }
        public long getNumberOfNotPermittedCalls() { return numberOfNotPermittedCalls; }
        public Instant getLastStateTransition() { return lastStateTransition; }

        public static class Builder {
            private CircuitBreakerState state = new CircuitBreakerState();

            public Builder endpointKey(String endpointKey) {
                state.endpointKey = endpointKey;
                return this;
            }

            public Builder state(String stateValue) {
                state.state = stateValue;
                return this;
            }

            public Builder failureRate(float failureRate) {
                state.failureRate = failureRate;
                return this;
            }

            public Builder slowCallRate(float slowCallRate) {
                state.slowCallRate = slowCallRate;
                return this;
            }

            public Builder numberOfCalls(int numberOfCalls) {
                state.numberOfCalls = numberOfCalls;
                return this;
            }

            public Builder numberOfFailedCalls(int numberOfFailedCalls) {
                state.numberOfFailedCalls = numberOfFailedCalls;
                return this;
            }

            public Builder numberOfSlowCalls(int numberOfSlowCalls) {
                state.numberOfSlowCalls = numberOfSlowCalls;
                return this;
            }

            public Builder numberOfNotPermittedCalls(long numberOfNotPermittedCalls) {
                state.numberOfNotPermittedCalls = numberOfNotPermittedCalls;
                return this;
            }

            public Builder lastStateTransition(Instant lastStateTransition) {
                state.lastStateTransition = lastStateTransition;
                return this;
            }

            public CircuitBreakerState build() {
                return state;
            }
        }
    }

    // Configuration properties getters and setters
    public int getFailureRateThreshold() { return failureRateThreshold; }
    public void setFailureRateThreshold(int failureRateThreshold) { this.failureRateThreshold = failureRateThreshold; }

    public int getSlowCallRateThreshold() { return slowCallRateThreshold; }
    public void setSlowCallRateThreshold(int slowCallRateThreshold) { this.slowCallRateThreshold = slowCallRateThreshold; }

    public Duration getSlowCallDurationThreshold() { return slowCallDurationThreshold; }
    public void setSlowCallDurationThreshold(Duration slowCallDurationThreshold) { this.slowCallDurationThreshold = slowCallDurationThreshold; }

    public int getMinimumNumberOfCalls() { return minimumNumberOfCalls; }
    public void setMinimumNumberOfCalls(int minimumNumberOfCalls) { this.minimumNumberOfCalls = minimumNumberOfCalls; }

    public int getPermittedNumberOfCallsInHalfOpenState() { return permittedNumberOfCallsInHalfOpenState; }
    public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) { this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState; }

    public Duration getWaitDurationInOpenState() { return waitDurationInOpenState; }
    public void setWaitDurationInOpenState(Duration waitDurationInOpenState) { this.waitDurationInOpenState = waitDurationInOpenState; }

    public int getSlidingWindowSize() { return slidingWindowSize; }
    public void setSlidingWindowSize(int slidingWindowSize) { this.slidingWindowSize = slidingWindowSize; }

    public String getSlidingWindowType() { return slidingWindowType; }
    public void setSlidingWindowType(String slidingWindowType) { this.slidingWindowType = slidingWindowType; }

    public boolean isAutomaticTransitionFromOpenToHalfOpenEnabled() { return automaticTransitionFromOpenToHalfOpenEnabled; }
    public void setAutomaticTransitionFromOpenToHalfOpenEnabled(boolean automaticTransitionFromOpenToHalfOpenEnabled) { this.automaticTransitionFromOpenToHalfOpenEnabled = automaticTransitionFromOpenToHalfOpenEnabled; }
}