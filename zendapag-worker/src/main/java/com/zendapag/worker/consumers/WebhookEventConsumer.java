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

import java.time.Instant;
import java.util.HashMap;

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
    private final MeterRegistry meterRegistry;

    private final Counter eventCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    private final Counter retryCounter;

    public WebhookEventConsumer(WebhookDeliveryService webhookDeliveryService,
                              DeadLetterQueueService deadLetterQueueService,
                              MeterRegistry meterRegistry) {
        this.webhookDeliveryService = webhookDeliveryService;
        this.deadLetterQueueService = deadLetterQueueService;
        this.meterRegistry = meterRegistry;

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
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleWebhookTriggered(@Payload WebhookTriggeredEvent event,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Processing WebhookTriggeredEvent: webhookId={}, merchantId={}, eventType={}, partition={}, offset={}",
                    event.getWebhookId(), event.getMerchantId(), event.getEventType(), partition, offset);

            // Use getTargetUrl() instead of getWebhookUrl()
            boolean deliverySuccess = webhookDeliveryService.attemptDelivery(
                event.getWebhookId(),
                event.getMerchantId(),
                event.getTargetUrl(),
                event.getEventType(),
                event.getPayload(),
                event.getHeaders(),
                null, // no signature field in this event
                1,    // default attempt number
                3     // default max attempts
            );

            eventCounter.increment();

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
            errorCounter.increment();
            throw e;
        } finally {
            sample.stop(processingTimer);
        }
    }

    @KafkaHandler
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleWebhookFailed(@Payload WebhookFailedEvent event,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Processing WebhookFailedEvent: webhookId={}, merchantId={}, attempt={}/{}, status={}, partition={}, offset={}",
                    event.getWebhookId(), event.getMerchantId(), event.getAttemptNumber(),
                    event.getMaxAttempts(), event.getHttpStatusCode(), partition, offset);

            if (event.willRetry()) {
                webhookDeliveryService.scheduleRetry(
                    event.getWebhookId(),
                    event.getDeliveryId(),
                    event.getNextRetryAt(),
                    event.getAttemptNumber() + 1
                );
                retryCounter.increment();
                log.info("Scheduled webhook retry: webhookId={}, nextAttempt={}, retryAt={}",
                        event.getWebhookId(), event.getAttemptNumber() + 1, event.getNextRetryAt());

            } else if (event.shouldGoToDeadLetterQueue()) {
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

            eventCounter.increment();
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process WebhookFailedEvent: webhookId={}, error={}",
                     event.getWebhookId(), e.getMessage(), e);
            errorCounter.increment();
            throw e;
        } finally {
            sample.stop(processingTimer);
        }
    }

    @KafkaHandler
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleWebhookRetry(@Payload WebhookRetryEvent event,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Processing WebhookRetryEvent: webhookId={}, attempt={}/{}, partition={}, offset={}",
                    event.getWebhookId(), event.getAttemptNumber(), event.getMaxAttempts(), partition, offset);

            // Check if retry is expired by comparing scheduledRetryAt with current time
            Instant scheduledAt = event.getScheduledRetryAt();
            boolean isExpired = scheduledAt != null && Instant.now().isAfter(scheduledAt.plusSeconds(300)); // 5 min grace

            if (isExpired) {
                log.warn("Webhook retry expired: webhookId={}", event.getWebhookId());
                deadLetterQueueService.sendExpiredRetryToDeadLetter(event);
                acknowledgment.acknowledge();
                return;
            }

            // Use webhookId as deliveryId since the event doesn't have deliveryId
            boolean deliverySuccess = webhookDeliveryService.retryDelivery(
                event.getWebhookId(),
                event.getWebhookId(), // use webhookId as fallback for deliveryId
                event.getAttemptNumber()
            );

            retryCounter.increment();
            eventCounter.increment();

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
            errorCounter.increment();
            throw e;
        } finally {
            sample.stop(processingTimer);
        }
    }

    @KafkaHandler(isDefault = true)
    public void handleUnknownEvent(Object event, Acknowledgment acknowledgment) {
        log.warn("Received unknown webhook event type: {}", event.getClass().getName());
        errorCounter.increment();
        acknowledgment.acknowledge();
    }
}
