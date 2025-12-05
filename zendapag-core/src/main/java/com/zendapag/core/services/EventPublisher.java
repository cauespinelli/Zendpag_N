package com.zendapag.core.services;

import com.zendapag.core.events.DomainEvent;
import com.zendapag.common.config.KafkaProducerConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Central event publisher service for domain events.
 * Provides reliable, transactional event publishing with metrics and error handling.
 */
@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProducerConfig.SyncKafkaPublisher syncPublisher;
    private final KafkaProducerConfig.AsyncKafkaPublisher asyncPublisher;
    private final KafkaProducerConfig.TransactionalKafkaPublisher transactionalPublisher;

    // Metrics
    private final Counter publishCounter;
    private final Counter errorCounter;
    private final Timer publishTimer;

    // Topic mapping
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    private static final String WEBHOOK_EVENTS_TOPIC = "webhook-events";
    private static final String SETTLEMENT_EVENTS_TOPIC = "settlement-events";
    private static final String RISK_EVENTS_TOPIC = "risk-analysis-events";
    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                         KafkaProducerConfig.SyncKafkaPublisher syncPublisher,
                         KafkaProducerConfig.AsyncKafkaPublisher asyncPublisher,
                         KafkaProducerConfig.TransactionalKafkaPublisher transactionalPublisher,
                         MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.syncPublisher = syncPublisher;
        this.asyncPublisher = asyncPublisher;
        this.transactionalPublisher = transactionalPublisher;

        this.publishCounter = Counter.builder("events.published")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("events.publish.errors")
                .register(meterRegistry);
        this.publishTimer = Timer.builder("events.publish.duration")
                .register(meterRegistry);
    }

    /**
     * Publish event asynchronously (fire and forget)
     * Best for high-throughput scenarios where immediate confirmation isn't needed
     */
    public void publishAsync(DomainEvent event) {
        publishAsync(event, null, null);
    }

    /**
     * Publish event asynchronously with callback handlers
     */
    public void publishAsync(DomainEvent event,
                           Runnable onSuccess,
                           Consumer<Exception> onFailure) {
        Timer.Sample sample = Timer.start();
        try {
            String topic = getTopicForEvent(event);
            String key = getKeyForEvent(event);

            log.debug("Publishing event asynchronously: type={}, id={}, topic={}",
                     event.getEventType(), event.getEventId(), topic);

            asyncPublisher.send(topic, key, event, onSuccess, onFailure);

            publishCounter.increment();

        } catch (Exception e) {
            log.error("Failed to publish event asynchronously: type={}, id={}, error={}",
                     event.getEventType(), event.getEventId(), e.getMessage(), e);

            errorCounter.increment();

            if (onFailure != null) {
                onFailure.accept(e);
            }
        } finally {
            sample.stop(publishTimer);
        }
    }

    /**
     * Publish event synchronously with confirmation
     * Best for critical events where confirmation is required
     */
    public SendResult<String, Object> publishSync(DomainEvent event) throws Exception {
        return publishSync(event, 30); // 30 second timeout
    }

    /**
     * Publish event synchronously with custom timeout
     */
    public SendResult<String, Object> publishSync(DomainEvent event, long timeoutSeconds) throws Exception {
        Timer.Sample sample = Timer.start();
        try {
            String topic = getTopicForEvent(event);
            String key = getKeyForEvent(event);

            log.debug("Publishing event synchronously: type={}, id={}, topic={}, timeout={}s",
                     event.getEventType(), event.getEventId(), topic, timeoutSeconds);

            SendResult<String, Object> result = syncPublisher.send(topic, key, event, timeoutSeconds);

            publishCounter.increment();

            log.debug("Event published successfully: type={}, id={}, partition={}, offset={}",
                     event.getEventType(), event.getEventId(),
                     result.getRecordMetadata().partition(),
                     result.getRecordMetadata().offset());

            return result;

        } catch (Exception e) {
            log.error("Failed to publish event synchronously: type={}, id={}, error={}",
                     event.getEventType(), event.getEventId(), e.getMessage(), e);

            errorCounter.increment();

            throw e;
        } finally {
            sample.stop(publishTimer);
        }
    }

    /**
     * Publish event within a transaction
     * Best for events that must be published as part of a database transaction
     */
    @Transactional
    public void publishInTransaction(DomainEvent event) {
        if (transactionalPublisher == null) {
            log.warn("Transactional publisher not available, falling back to async publish");
            publishAsync(event);
            return;
        }

        Timer.Sample sample = Timer.start();
        try {
            String topic = getTopicForEvent(event);
            String key = getKeyForEvent(event);

            log.debug("Publishing event in transaction: type={}, id={}, topic={}",
                     event.getEventType(), event.getEventId(), topic);

            transactionalPublisher.executeInTransaction(() -> {
                kafkaTemplate.send(topic, key, event);
            });

            publishCounter.increment();

            log.debug("Event published in transaction: type={}, id={}",
                     event.getEventType(), event.getEventId());

        } catch (Exception e) {
            log.error("Failed to publish event in transaction: type={}, id={}, error={}",
                     event.getEventType(), event.getEventId(), e.getMessage(), e);

            errorCounter.increment();

            throw new RuntimeException("Failed to publish event in transaction", e);
        } finally {
            sample.stop(publishTimer);
        }
    }

    /**
     * Bulk publish multiple events asynchronously
     * Optimized for high-throughput scenarios
     */
    public CompletableFuture<Void> publishBulkAsync(DomainEvent... events) {
        CompletableFuture<?>[] futures = new CompletableFuture[events.length];

        for (int i = 0; i < events.length; i++) {
            DomainEvent event = events[i];
            String topic = getTopicForEvent(event);
            String key = getKeyForEvent(event);

            futures[i] = kafkaTemplate.send(topic, key, event);
        }

        return CompletableFuture.allOf(futures)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Bulk publish failed: {}", throwable.getMessage(), throwable);
                        errorCounter.increment();
                    } else {
                        publishCounter.increment();
                        log.debug("Bulk published {} events successfully", events.length);
                    }
                });
    }

    /**
     * Get appropriate Kafka topic for event type
     */
    private String getTopicForEvent(DomainEvent event) {
        String eventType = event.getEventType();

        if (eventType.startsWith("payment_")) {
            return PAYMENT_EVENTS_TOPIC;
        } else if (eventType.startsWith("webhook_")) {
            return WEBHOOK_EVENTS_TOPIC;
        } else if (eventType.startsWith("settlement_")) {
            return SETTLEMENT_EVENTS_TOPIC;
        } else if (eventType.startsWith("risk_") || eventType.startsWith("fraud_")) {
            return RISK_EVENTS_TOPIC;
        } else if (eventType.startsWith("notification_") || eventType.startsWith("email_") || eventType.startsWith("sms_")) {
            return NOTIFICATION_EVENTS_TOPIC;
        } else {
            log.warn("Unknown event type {}, using default topic", eventType);
            return PAYMENT_EVENTS_TOPIC; // Default fallback
        }
    }

    /**
     * Get Kafka partition key for event
     * Events with the same key will be processed in order
     */
    private String getKeyForEvent(DomainEvent event) {
        // Use aggregate ID for ordering if available
        if (event.getAggregateId() != null) {
            return event.getAggregateId();
        }

        // Fall back to merchant ID from metadata for merchant-level ordering
        Object merchantId = event.getMetadataValue("merchant_id");
        if (merchantId != null) {
            return merchantId.toString();
        }

        // Last resort: use event ID (no ordering guarantees)
        return event.getEventId();
    }

    /**
     * Publish event with retry logic for critical scenarios
     */
    public void publishWithRetry(DomainEvent event, int maxRetries) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= maxRetries) {
            try {
                publishSync(event, 10); // Short timeout for retries
                return; // Success
            } catch (Exception e) {
                lastException = e;
                attempts++;

                if (attempts <= maxRetries) {
                    long delay = Math.min(1000L * (1L << (attempts - 1)), 10000L); // Exponential backoff, max 10s
                    log.warn("Event publish attempt {} failed, retrying in {}ms: type={}, id={}, error={}",
                            attempts, delay, event.getEventType(), event.getEventId(), e.getMessage());

                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry delay", ie);
                    }
                }
            }
        }

        log.error("Failed to publish event after {} attempts: type={}, id={}",
                 maxRetries + 1, event.getEventType(), event.getEventId(), lastException);
        throw new RuntimeException("Failed to publish event after retries", lastException);
    }

    /**
     * Check if event publishing is healthy
     */
    public boolean isHealthy() {
        try {
            // Simple health check - try to get metadata
            kafkaTemplate.partitionsFor(PAYMENT_EVENTS_TOPIC);
            return true;
        } catch (Exception e) {
            log.error("Event publisher health check failed: {}", e.getMessage());
            return false;
        }
    }
}