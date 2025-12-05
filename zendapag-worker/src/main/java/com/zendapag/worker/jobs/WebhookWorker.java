package com.zendapag.worker.jobs;

import com.zendapag.core.entity.Webhook;
import com.zendapag.core.entity.enums.WebhookStatus;
import com.zendapag.core.events.WebhookDeliveredEvent;
import com.zendapag.core.events.WebhookFailedEvent;
import com.zendapag.core.events.WebhookTriggeredEvent;
import com.zendapag.core.repository.WebhookRepository;
import com.zendapag.worker.services.WebhookDeliveryService;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebhookWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookWorker.class);

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryService webhookDeliveryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    private final Counter deliverySuccessCounter;
    private final Counter deliveryFailureCounter;
    private final Counter retryCounter;
    private final Timer deliveryTimer;

    private final Map<String, Integer> merchantRetryCount = new ConcurrentHashMap<>();

    public WebhookWorker(WebhookRepository webhookRepository,
                        WebhookDeliveryService webhookDeliveryService,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        MeterRegistry meterRegistry,
                        CircuitBreakerRegistry circuitBreakerRegistry,
                        RateLimiterRegistry rateLimiterRegistry,
                        ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.webhookDeliveryService = webhookDeliveryService;
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("webhook-delivery");
        this.rateLimiter = rateLimiterRegistry.rateLimiter("webhook-delivery");
        this.objectMapper = objectMapper;

        this.deliverySuccessCounter = Counter.builder("webhook.deliveries.success")
                .description("Successful webhook deliveries")
                .register(meterRegistry);
        this.deliveryFailureCounter = Counter.builder("webhook.deliveries.failure")
                .description("Failed webhook deliveries")
                .register(meterRegistry);
        this.retryCounter = Counter.builder("webhook.deliveries.retry")
                .description("Webhook delivery retries")
                .register(meterRegistry);
        this.deliveryTimer = Timer.builder("webhook.delivery.duration")
                .description("Webhook delivery duration")
                .register(meterRegistry);
    }

    @KafkaListener(
        topics = "webhook-triggered",
        groupId = "webhook-worker",
        containerFactory = "webhookKafkaListenerContainerFactory"
    )
    public void processWebhookTriggered(@Payload WebhookTriggeredEvent event,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                        @Header(KafkaHeaders.OFFSET) long offset,
                                        Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String webhookId = event.getWebhookId();
        String merchantId = event.getMerchantId();

        log.info("Processing webhook triggered event: webhookId={}, merchantId={}, eventType={}, partition={}, offset={}",
                webhookId, merchantId, event.getEventType(), partition, offset);

        try {
            Webhook webhook = webhookRepository.findById(UUID.fromString(webhookId))
                    .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + webhookId));

            boolean delivered = circuitBreaker.executeSupplier(() ->
                rateLimiter.executeSupplier(() ->
                    webhookDeliveryService.attemptDelivery(
                        webhookId,
                        merchantId,
                        event.getTargetUrl(),
                        event.getEventType(),
                        event.getPayload(),
                        event.getHeaders(),
                        webhook.getSignature(),
                        webhook.getRetryCount() + 1,
                        webhook.getMaxRetries()
                    )
                )
            );

            if (delivered) {
                handleSuccessfulDelivery(webhook, event);
            } else {
                handleFailedDelivery(webhook, event, "Delivery returned false", null);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing webhook: webhookId={}, error={}", webhookId, e.getMessage(), e);

            try {
                Webhook webhook = webhookRepository.findById(UUID.fromString(webhookId)).orElse(null);
                if (webhook != null) {
                    handleFailedDelivery(webhook, event, e.getMessage(), e.getClass().getSimpleName());
                }
            } catch (Exception inner) {
                log.error("Failed to handle webhook failure: {}", inner.getMessage(), inner);
            }

            acknowledgment.acknowledge();
        } finally {
            sample.stop(deliveryTimer);
        }
    }

    private void handleSuccessfulDelivery(Webhook webhook, WebhookTriggeredEvent event) {
        webhook.setStatus(WebhookStatus.DELIVERED);
        webhook.setDeliveredAt(Instant.now());
        webhookRepository.save(webhook);

        deliverySuccessCounter.increment();

        // WebhookDeliveredEvent(webhookId, merchantId, httpStatusCode, responseTimeMs, responseBody, attemptNumber, correlationId, causationId, aggregateVersion)
        WebhookDeliveredEvent deliveredEvent = new WebhookDeliveredEvent(
            event.getWebhookId(),
            event.getMerchantId(),
            200,                           // httpStatusCode
            0L,                            // responseTimeMs
            null,                          // responseBody
            webhook.getRetryCount() + 1,   // attemptNumber
            event.getCorrelationId(),      // correlationId
            event.getEventId(),            // causationId
            null                           // aggregateVersion
        );

        kafkaTemplate.send("webhook-delivered", event.getWebhookId(), deliveredEvent);

        log.info("Webhook delivered successfully: webhookId={}, merchantId={}",
                event.getWebhookId(), event.getMerchantId());
    }

    private void handleFailedDelivery(Webhook webhook, WebhookTriggeredEvent event,
                                     String errorMessage, String errorCode) {
        int currentRetry = webhook.getRetryCount();
        int maxRetries = webhook.getMaxRetries();

        webhook.markAsFailed(errorMessage, errorCode);
        webhookRepository.save(webhook);

        deliveryFailureCounter.increment();

        if (currentRetry < maxRetries) {
            scheduleRetry(webhook, event, currentRetry + 1, maxRetries);
        } else {
            log.warn("Max retries reached for webhook: webhookId={}, attempts={}",
                    event.getWebhookId(), currentRetry);

            WebhookFailedEvent failedEvent = createFailedEvent(
                event, webhook.getId().toString(), currentRetry, maxRetries,
                errorMessage, null, event.getCorrelationId(), event.getEventId()
            );

            kafkaTemplate.send("webhook-failed", event.getWebhookId(), failedEvent);
        }
    }

    private void scheduleRetry(Webhook webhook, WebhookTriggeredEvent event, int attemptNumber, int maxRetries) {
        long delayMs = calculateBackoff(attemptNumber);
        Instant nextRetryAt = Instant.now().plusMillis(delayMs);

        retryCounter.increment();

        WebhookFailedEvent failedEvent = createFailedEventWithRetry(
            event, webhook.getId().toString(), attemptNumber, maxRetries,
            "Scheduling retry", nextRetryAt, event.getCorrelationId(), event.getEventId()
        );

        kafkaTemplate.send("webhook-failed", event.getWebhookId(), failedEvent);

        log.info("Scheduled webhook retry: webhookId={}, attempt={}, nextRetryAt={}",
                event.getWebhookId(), attemptNumber, nextRetryAt);
    }

    private WebhookFailedEvent createFailedEvent(WebhookTriggeredEvent event, String deliveryId,
            int attemptNumber, int maxAttempts, String errorMessage, Instant nextRetryAt,
            String correlationId, String causationId) {
        return new WebhookFailedEvent(
            event.getWebhookId(),
            event.getMerchantId(),
            deliveryId,
            event.getEventType(),
            correlationId,
            event.getTargetUrl(),
            Instant.now(),
            attemptNumber,
            maxAttempts,
            0,
            errorMessage,
            null,
            0L,
            nextRetryAt,
            "processing_error",
            event.getHeaders(),
            null,
            event.getPayload(),
            correlationId,
            causationId
        );
    }

    private WebhookFailedEvent createFailedEventWithRetry(WebhookTriggeredEvent event, String deliveryId,
            int attemptNumber, int maxAttempts, String errorMessage, Instant nextRetryAt,
            String correlationId, String causationId) {
        return new WebhookFailedEvent(
            event.getWebhookId(),
            event.getMerchantId(),
            deliveryId,
            event.getEventType(),
            correlationId,
            event.getTargetUrl(),
            Instant.now(),
            attemptNumber,
            maxAttempts,
            0,
            errorMessage,
            null,
            0L,
            nextRetryAt,
            "retry_scheduled",
            event.getHeaders(),
            null,
            event.getPayload(),
            correlationId,
            causationId
        );
    }

    private long calculateBackoff(int attemptNumber) {
        long baseDelay = 1000L;
        double multiplier = 2.0;
        long maxDelay = 300000L;

        long delay = (long) (baseDelay * Math.pow(multiplier, attemptNumber - 1));
        return Math.min(delay, maxDelay);
    }

    @Scheduled(fixedRate = 60000)
    public void processFailedWebhooks() {
        log.debug("Processing failed webhooks for retry");

        try {
            List<Webhook> failedWebhooks = webhookRepository.findFailedWebhooksReadyForRetry(Instant.now());

            for (Webhook webhook : failedWebhooks) {
                try {
                    String merchantId = webhook.getMerchant() != null
                        ? webhook.getMerchant().getId().toString()
                        : "unknown";

                    String payloadStr = serializePayload(webhook.getPayload());

                    WebhookTriggeredEvent retryEvent = new WebhookTriggeredEvent(
                        webhook.getId().toString(),
                        merchantId,
                        webhook.getEventType(),
                        webhook.getUrl(),
                        payloadStr,
                        webhook.getHeaders(),
                        UUID.randomUUID().toString()
                    );

                    kafkaTemplate.send("webhook-triggered", webhook.getId().toString(), retryEvent);

                    log.info("Queued failed webhook for retry: webhookId={}, retryCount={}",
                            webhook.getId(), webhook.getRetryCount());

                } catch (Exception e) {
                    log.error("Failed to queue webhook for retry: webhookId={}, error={}",
                            webhook.getId(), e.getMessage(), e);
                }
            }

            if (!failedWebhooks.isEmpty()) {
                log.info("Processed {} failed webhooks for retry", failedWebhooks.size());
            }

        } catch (Exception e) {
            log.error("Error processing failed webhooks: {}", e.getMessage(), e);
        }
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize payload: {}", e.getMessage());
            return "{}";
        }
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupMerchantRetryCount() {
        merchantRetryCount.clear();
        log.debug("Cleared merchant retry count cache");
    }
}
