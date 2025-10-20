package com.zendapag.worker.consumers;

import com.zendapag.core.events.WebhookTriggeredEvent;
import com.zendapag.core.events.WebhookFailedEvent;
import com.zendapag.core.events.WebhookRetryEvent;
import com.zendapag.worker.services.WebhookDeliveryService;
import com.zendapag.worker.services.DeadLetterQueueService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for webhook-related events.
 * Handles webhook delivery, retries, and failure processing with low latency.
 */
@Component
@KafkaListener(
    topics = "webhook-events",
    groupId = "webhook-processor",
    containerFactory = "webhookEventsContainerFactory"
)
public class WebhookEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventConsumer.class);

    private final WebhookDeliveryService webhookDeliveryService;
    private final DeadLetterQueueService deadLetterQueueService;

    // Metrics
    private final Counter eventCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    private final Counter retryCounter;

    public WebhookEventConsumer(WebhookDeliveryService webhookDeliveryService,
                              DeadLetterQueueService deadLetterQueueService,
                              MeterRegistry meterRegistry) {
        this.webhookDeliveryService = webhookDeliveryService;
        this.deadLetterQueueService = deadLetterQueueService;

        this.eventCounter = Counter.builder("kafka.events.processed")
                .tag("topic", "webhook-events")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("kafka.events.error")
                .tag("topic", "webhook-events")
                .register(meterRegistry);
        this.processingTimer = Timer.builder("kafka.events.processing.time")
                .tag("topic", "webhook-events")
                .register(meterRegistry);
        this.retryCounter = Counter.builder("webhook.retries")
                .register(meterRegistry);
    }

    @KafkaHandler
    @Retryable(
        value = {Exception.class},
        maxAttempts = 2, // Fewer retries for webhooks to maintain low latency
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void handleWebhookTriggered(@Payload WebhookTriggeredEvent event,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        try {
            log.info("Processing WebhookTriggeredEvent: webhookId={}, merchantId={}, eventType={}, partition={}, offset={}",
                    event.getWebhookId(), event.getMerchantId(), event.getEventType(), partition, offset);

            // Attempt webhook delivery
            boolean deliverySuccess = webhookDeliveryService.attemptDelivery(
                event.getWebhookId(),
                event.getMerchantId(),
                event.getWebhookUrl(),
                event.getEventType(),
                event.getPayload(),
                event.getHeaders(),
                event.getSignature(),
                event.getAttemptNumber(),
                event.getMaxAttempts()
            );

            // Track metrics
            eventCounter.increment(
                "event_type", "webhook_triggered",
                "merchant_id", event.getMerchantId(),
                "webhook_event_type", event.getEventType(),
                "delivery_success", String.valueOf(deliverySuccess)
            );

            if (deliverySuccess) {
                log.debug("Successfully delivered webhook: webhookId={}, eventType={}",
                         event.getWebhookId(), event.getEventType());
            } else {
                log.warn("Webhook delivery failed: webhookId={}, eventType={}, will be retried or sent to DLQ",
                        event.getWebhookId(), event.getEventType());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process WebhookTriggeredEvent: webhookId={}, error={}",
                     event.getWebhookId(), e.getMessage(), e);

            errorCounter.increment(
                "event_type", "webhook_triggered",
                "error_type", e.getClass().getSimpleName()
            );

            throw e;
        } finally {
            sample.stop(processingTimer.tag("event_type", "webhook_triggered"));
        }
    }

    @KafkaHandler
    @Retryable(
        value = {Exception.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleWebhookFailed(@Payload WebhookFailedEvent event,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        try {
            log.info("Processing WebhookFailedEvent: webhookId={}, merchantId={}, attempt={}/{}, status={}, partition={}, offset={}",
                    event.getWebhookId(), event.getMerchantId(), event.getAttemptNumber(),
                    event.getMaxAttempts(), event.getHttpStatusCode(), partition, offset);

            if (event.willRetry()) {
                // Schedule retry
                webhookDeliveryService.scheduleRetry(
                    event.getWebhookId(),
                    event.getDeliveryId(),
                    event.getNextRetryAt(),
                    event.getAttemptNumber() + 1
                );

                retryCounter.increment(
                    "webhook_id", event.getWebhookId(),
                    "merchant_id", event.getMerchantId(),
                    "failure_category", event.categorizeFailure()
                );

                log.info("Scheduled webhook retry: webhookId={}, nextAttempt={}, retryAt={}",
                        event.getWebhookId(), event.getAttemptNumber() + 1, event.getNextRetryAt());

            } else if (event.shouldGoToDeadLetterQueue()) {
                // Send to dead letter queue
                deadLetterQueueService.sendWebhookToDeadLetter(
                    event.getWebhookId(),
                    event.getDeliveryId(),
                    event.getOriginalEventType(),
                    event.getOriginalEventId(),
                    event.getWebhookUrl(),
                    event.getErrorMessage(),
                    event.getHttpStatusCode(),
                    event.getAttemptNumber(),
                    event.getResponseBody()
                );

                log.warn("Webhook sent to dead letter queue: webhookId={}, finalAttempt={}, status={}",
                        event.getWebhookId(), event.getAttemptNumber(), event.getHttpStatusCode());
            }

            // Track metrics
            eventCounter.increment(
                "event_type", "webhook_failed",
                "merchant_id", event.getMerchantId(),
                "failure_category", event.categorizeFailure(),
                "will_retry", String.valueOf(event.willRetry()),
                "is_final_attempt", String.valueOf(event.isLastAttempt())
            );

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process WebhookFailedEvent: webhookId={}, error={}",
                     event.getWebhookId(), e.getMessage(), e);

            errorCounter.increment(
                "event_type", "webhook_failed",
                "error_type", e.getClass().getSimpleName()
            );

            throw e;
        } finally {
            sample.stop(processingTimer.tag("event_type", "webhook_failed"));
        }
    }

    @KafkaHandler
    @Retryable(
        value = {Exception.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void handleWebhookRetry(@Payload WebhookRetryEvent event,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        try {
            log.info("Processing WebhookRetryEvent: webhookId={}, deliveryId={}, attempt={}, partition={}, offset={}",
                    event.getWebhookId(), event.getDeliveryId(), event.getAttemptNumber(), partition, offset);

            // Check if retry is still valid (not expired)
            if (event.isRetryExpired()) {
                log.warn("Webhook retry expired: webhookId={}, deliveryId={}",
                        event.getWebhookId(), event.getDeliveryId());

                // Send to dead letter queue
                deadLetterQueueService.sendExpiredRetryToDeadLetter(event);
                acknowledgment.acknowledge();
                return;
            }

            // Attempt webhook delivery again
            boolean deliverySuccess = webhookDeliveryService.retryDelivery(
                event.getWebhookId(),
                event.getDeliveryId(),
                event.getAttemptNumber()
            );

            // Track retry metrics
            retryCounter.increment(
                "webhook_id", event.getWebhookId(),
                "merchant_id", event.getMerchantId(),
                "retry_success", String.valueOf(deliverySuccess)
            );

            eventCounter.increment(
                "event_type", "webhook_retry",
                "merchant_id", event.getMerchantId(),
                "retry_success", String.valueOf(deliverySuccess)
            );

            if (deliverySuccess) {
                log.info("Webhook retry successful: webhookId={}, attempt={}",
                        event.getWebhookId(), event.getAttemptNumber());
            } else {
                log.warn("Webhook retry failed: webhookId={}, attempt={}",
                        event.getWebhookId(), event.getAttemptNumber());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process WebhookRetryEvent: webhookId={}, error={}",
                     event.getWebhookId(), e.getMessage(), e);

            errorCounter.increment(
                "event_type", "webhook_retry",
                "error_type", e.getClass().getSimpleName()
            );

            throw e;
        } finally {
            sample.stop(processingTimer.tag("event_type", "webhook_retry"));
        }
    }

    // Handler for unknown event types
    @KafkaHandler(isDefault = true)
    public void handleUnknownEvent(Object event, Acknowledgment acknowledgment) {
        log.warn("Received unknown webhook event type: {}", event.getClass().getName());

        errorCounter.increment(
            "event_type", "unknown",
            "error_type", "UnknownEventType"
        );

        // Acknowledge to avoid infinite retries
        acknowledgment.acknowledge();
    }
}