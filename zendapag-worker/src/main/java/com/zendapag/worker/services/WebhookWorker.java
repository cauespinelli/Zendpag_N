package com.zendapag.worker.services;

import com.zendapag.core.entity.Webhook;
import com.zendapag.core.enums.WebhookStatus;
import com.zendapag.core.events.WebhookDeliveredEvent;
import com.zendapag.core.events.WebhookFailedEvent;
import com.zendapag.core.events.WebhookTriggeredEvent;
import com.zendapag.core.repository.WebhookRepository;
import com.zendapag.core.services.EventPublisher;
import com.zendapag.worker.config.WebhookConfig;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Webhook Worker - Processa eventos de webhook com alta confiabilidade
 * Implementa retry strategy, circuit breaker, rate limiting e monitoramento completo
 */
@Component
@Slf4j
public class WebhookWorker {

    private final WebhookRepository webhookRepository;
    private final WebhookSigner webhookSigner;
    private final RestTemplate webhookRestTemplate;
    private final EventPublisher eventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final WebhookConfig webhookConfig;

    // Metrics
    private final Counter webhookDeliveryCounter;
    private final Counter webhookErrorCounter;
    private final Timer webhookDeliveryTimer;
    private final Counter retryCounter;

    @Autowired
    public WebhookWorker(WebhookRepository webhookRepository,
                        WebhookSigner webhookSigner,
                        RestTemplate webhookRestTemplate,
                        EventPublisher eventPublisher,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        CircuitBreakerRegistry circuitBreakerRegistry,
                        RateLimiterRegistry rateLimiterRegistry,
                        WebhookConfig webhookConfig,
                        MeterRegistry meterRegistry) {
        this.webhookRepository = webhookRepository;
        this.webhookSigner = webhookSigner;
        this.webhookRestTemplate = webhookRestTemplate;
        this.eventPublisher = eventPublisher;
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.webhookConfig = webhookConfig;

        // Initialize metrics
        this.webhookDeliveryCounter = Counter.builder("webhook.deliveries")
                .register(meterRegistry);
        this.webhookErrorCounter = Counter.builder("webhook.errors")
                .register(meterRegistry);
        this.webhookDeliveryTimer = Timer.builder("webhook.delivery.duration")
                .register(meterRegistry);
        this.retryCounter = Counter.builder("webhook.retries")
                .register(meterRegistry);
    }

    /**
     * Processa eventos de webhook vindos do Kafka
     */
    @KafkaListener(
        topics = "webhook-events",
        groupId = "webhook-processor",
        containerFactory = "webhookEventsContainerFactory"
    )
    @Transactional
    @Retryable(
        value = {Exception.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void processWebhookEvent(@Payload WebhookTriggeredEvent event,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {

        String webhookId = event.getWebhookId();
        String deliveryId = UUID.randomUUID().toString();

        log.info("Processing webhook event: webhookId={}, eventType={}, merchantId={}, partition={}, offset={}",
                webhookId, event.getEventType(), event.getMerchantId(), partition, offset);

        Timer.Sample sample = Timer.start();
        boolean deliverySuccess = false;

        try {
            Optional<Webhook> webhookOpt = webhookRepository.findById(webhookId);
            if (webhookOpt.isEmpty()) {
                log.warn("Webhook not found: webhookId={}", webhookId);
                acknowledgment.acknowledge();
                return;
            }

            Webhook webhook = webhookOpt.get();

            // Check if webhook is active
            if (!webhook.isActive()) {
                log.info("Webhook is inactive, skipping delivery: webhookId={}", webhookId);
                acknowledgment.acknowledge();
                return;
            }

            // Apply rate limiting per merchant
            RateLimiter rateLimiter = getRateLimiterForMerchant(event.getMerchantId());
            if (!rateLimiter.acquirePermission()) {
                log.warn("Rate limit exceeded for merchant: merchantId={}, webhookId={}",
                        event.getMerchantId(), webhookId);
                scheduleRetry(event, "Rate limit exceeded");
                acknowledgment.acknowledge();
                return;
            }

            // Apply circuit breaker per endpoint
            CircuitBreaker circuitBreaker = getCircuitBreakerForEndpoint(webhook.getUrl());

            deliverySuccess = circuitBreaker.executeSupplier(() -> {
                return deliverWebhook(webhook, event, deliveryId);
            });

            if (deliverySuccess) {
                handleSuccessfulDelivery(webhook, event, deliveryId);
            } else {
                handleFailedDelivery(webhook, event, deliveryId, "Delivery failed");
            }

        } catch (Exception e) {
            log.error("Error processing webhook event: webhookId={}, error={}", webhookId, e.getMessage(), e);

            webhookErrorCounter.increment(
                "webhook_id", webhookId,
                "error_type", e.getClass().getSimpleName(),
                "merchant_id", event.getMerchantId()
            );

            handleFailedDelivery(null, event, deliveryId, e.getMessage());
            throw e; // Re-throw to trigger retry mechanism

        } finally {
            sample.stop(webhookDeliveryTimer.tag("success", String.valueOf(deliverySuccess)));
            acknowledgment.acknowledge();
        }
    }

    /**
     * Entrega o webhook para o endpoint do merchant
     */
    private boolean deliverWebhook(Webhook webhook, WebhookTriggeredEvent event, String deliveryId) {
        Timer.Sample deliveryTimer = Timer.start();
        boolean success = false;

        try {
            log.debug("Delivering webhook: webhookId={}, url={}, deliveryId={}",
                     webhook.getId(), webhook.getUrl(), deliveryId);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "Zendapag-Webhooks/1.0");
            headers.set("X-Zendapag-Webhook-Id", webhook.getId());
            headers.set("X-Zendapag-Delivery-Id", deliveryId);
            headers.set("X-Zendapag-Event-Type", event.getEventType());
            headers.set("X-Zendapag-Event-Id", event.getEventId());
            headers.set("X-Zendapag-Timestamp", Instant.now().toString());

            // Generate signature
            String payload = event.getPayload();
            String signature = webhookSigner.generateSignature(payload);
            headers.set("X-Zendapag-Signature", signature);

            // Add custom headers if provided
            if (event.getHeaders() != null) {
                event.getHeaders().forEach(headers::set);
            }

            HttpEntity<String> request = new HttpEntity<>(payload, headers);

            // Make the request with timeout
            ResponseEntity<String> response = webhookRestTemplate.exchange(
                webhook.getUrl(),
                HttpMethod.POST,
                request,
                String.class
            );

            // Consider 2xx as success
            success = response.getStatusCode().is2xxSuccessful();

            if (success) {
                log.info("Webhook delivered successfully: webhookId={}, deliveryId={}, status={}",
                        webhook.getId(), deliveryId, response.getStatusCode());
            } else {
                log.warn("Webhook delivery returned non-success status: webhookId={}, deliveryId={}, status={}",
                        webhook.getId(), deliveryId, response.getStatusCode());
            }

            webhookDeliveryCounter.increment(
                "webhook_id", webhook.getId(),
                "merchant_id", event.getMerchantId(),
                "status", success ? "success" : "failed",
                "http_status", String.valueOf(response.getStatusCode().value())
            );

            return success;

        } catch (HttpStatusCodeException e) {
            log.warn("Webhook delivery failed with HTTP error: webhookId={}, deliveryId={}, status={}, response={}",
                    webhook.getId(), deliveryId, e.getStatusCode(), e.getResponseBodyAsString());

            webhookDeliveryCounter.increment(
                "webhook_id", webhook.getId(),
                "merchant_id", event.getMerchantId(),
                "status", "failed",
                "http_status", String.valueOf(e.getStatusCode().value())
            );

            return false;

        } catch (ResourceAccessException e) {
            log.warn("Webhook delivery failed with network error: webhookId={}, deliveryId={}, error={}",
                    webhook.getId(), deliveryId, e.getMessage());

            webhookDeliveryCounter.increment(
                "webhook_id", webhook.getId(),
                "merchant_id", event.getMerchantId(),
                "status", "failed",
                "error_type", "network"
            );

            return false;

        } catch (Exception e) {
            log.error("Webhook delivery failed with unexpected error: webhookId={}, deliveryId={}, error={}",
                     webhook.getId(), deliveryId, e.getMessage(), e);

            webhookDeliveryCounter.increment(
                "webhook_id", webhook.getId(),
                "merchant_id", event.getMerchantId(),
                "status", "failed",
                "error_type", "unexpected"
            );

            return false;

        } finally {
            deliveryTimer.stop(Timer.builder("webhook.http.request.duration")
                    .tag("webhook_id", webhook.getId())
                    .tag("success", String.valueOf(success))
                    .register(io.micrometer.core.instrument.Metrics.globalRegistry));
        }
    }

    /**
     * Trata entrega bem-sucedida do webhook
     */
    private void handleSuccessfulDelivery(Webhook webhook, WebhookTriggeredEvent event, String deliveryId) {
        try {
            // Update webhook status
            webhook.setStatus(WebhookStatus.ACTIVE);
            webhook.setLastDeliveryAt(Instant.now());
            webhook.incrementSuccessfulDeliveries();
            webhookRepository.save(webhook);

            // Publish success event
            WebhookDeliveredEvent deliveredEvent = new WebhookDeliveredEvent(
                webhook.getId(),
                event.getMerchantId(),
                deliveryId,
                event.getEventType(),
                event.getEventId(),
                webhook.getUrl(),
                Instant.now(),
                event.getAttemptNumber(),
                true,
                200,
                "Delivered successfully",
                event.getCorrelationId(),
                event.getCausationId()
            );

            eventPublisher.publishAsync(deliveredEvent);

            log.debug("Webhook delivery success handled: webhookId={}, deliveryId={}", webhook.getId(), deliveryId);

        } catch (Exception e) {
            log.error("Error handling successful webhook delivery: webhookId={}, error={}",
                     webhook.getId(), e.getMessage(), e);
        }
    }

    /**
     * Trata falha na entrega do webhook
     */
    private void handleFailedDelivery(Webhook webhook, WebhookTriggeredEvent event, String deliveryId, String errorMessage) {
        try {
            int attemptNumber = event.getAttemptNumber();
            int maxAttempts = webhookConfig.getMaxRetryAttempts();

            // Update webhook if available
            if (webhook != null) {
                webhook.incrementFailedDeliveries();
                webhook.setLastErrorAt(Instant.now());
                webhook.setLastError(errorMessage);
                webhookRepository.save(webhook);
            }

            // Determine if retry should be attempted
            boolean willRetry = attemptNumber < maxAttempts && shouldRetry(errorMessage);
            Instant nextRetryAt = willRetry ? calculateNextRetryTime(attemptNumber) : null;

            // Publish failure event
            WebhookFailedEvent failedEvent = new WebhookFailedEvent(
                event.getWebhookId(),
                event.getMerchantId(),
                deliveryId,
                event.getEventType(),
                event.getEventId(),
                event.getWebhookUrl(),
                Instant.now(),
                attemptNumber,
                maxAttempts,
                extractHttpStatusCode(errorMessage),
                errorMessage,
                null, // response body
                0L, // response time
                nextRetryAt,
                categorizeFailure(errorMessage),
                null, // request headers
                null, // response headers
                event.getPayload(),
                event.getCorrelationId(),
                event.getCausationId()
            );

            if (willRetry) {
                // Schedule retry
                scheduleRetry(event.toBuilder()
                        .attemptNumber(attemptNumber + 1)
                        .build(),
                        Duration.between(Instant.now(), nextRetryAt));

                retryCounter.increment(
                    "webhook_id", event.getWebhookId(),
                    "merchant_id", event.getMerchantId(),
                    "attempt", String.valueOf(attemptNumber + 1)
                );
            }

            eventPublisher.publishAsync(failedEvent);

            log.warn("Webhook delivery failure handled: webhookId={}, deliveryId={}, attempt={}/{}, willRetry={}",
                    event.getWebhookId(), deliveryId, attemptNumber, maxAttempts, willRetry);

        } catch (Exception e) {
            log.error("Error handling webhook delivery failure: webhookId={}, error={}",
                     event.getWebhookId(), e.getMessage(), e);
        }
    }

    /**
     * Agenda retry do webhook com delay
     */
    private void scheduleRetry(WebhookTriggeredEvent event, Duration delay) {
        CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> {
                    try {
                        kafkaTemplate.send("webhook-events", event.getWebhookId(), event);
                        log.info("Webhook retry scheduled: webhookId={}, attempt={}, delay={}ms",
                                event.getWebhookId(), event.getAttemptNumber(), delay.toMillis());
                    } catch (Exception e) {
                        log.error("Failed to schedule webhook retry: webhookId={}, error={}",
                                 event.getWebhookId(), e.getMessage(), e);
                    }
                });
    }

    private void scheduleRetry(WebhookTriggeredEvent event, String reason) {
        Duration delay = calculateRetryDelay(event.getAttemptNumber());
        scheduleRetry(event, delay);
    }

    /**
     * Calcula o próximo horário de retry usando exponential backoff
     */
    private Instant calculateNextRetryTime(int attemptNumber) {
        Duration delay = calculateRetryDelay(attemptNumber);
        return Instant.now().plus(delay);
    }

    private Duration calculateRetryDelay(int attemptNumber) {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s (max 30s)
        long delaySeconds = Math.min(
                webhookConfig.getInitialRetryDelaySeconds() * (long) Math.pow(2, attemptNumber - 1),
                webhookConfig.getMaxRetryDelaySeconds()
        );
        return Duration.ofSeconds(delaySeconds);
    }

    /**
     * Determina se o erro permite retry
     */
    private boolean shouldRetry(String errorMessage) {
        if (errorMessage == null) {
            return true;
        }

        String message = errorMessage.toLowerCase();

        // Don't retry client errors (4xx), except for specific cases
        if (message.contains("400") || message.contains("401") ||
            message.contains("403") || message.contains("404")) {
            return false;
        }

        // Retry for rate limiting and temporary errors
        if (message.contains("429") || message.contains("timeout") ||
            message.contains("502") || message.contains("503") || message.contains("504")) {
            return true;
        }

        // Retry for network errors
        if (message.contains("connection") || message.contains("network")) {
            return true;
        }

        return true; // Default to retry
    }

    /**
     * Extrai código HTTP da mensagem de erro
     */
    private int extractHttpStatusCode(String errorMessage) {
        if (errorMessage == null) return 0;

        // Try to extract HTTP status codes
        if (errorMessage.contains("400")) return 400;
        if (errorMessage.contains("401")) return 401;
        if (errorMessage.contains("403")) return 403;
        if (errorMessage.contains("404")) return 404;
        if (errorMessage.contains("429")) return 429;
        if (errorMessage.contains("500")) return 500;
        if (errorMessage.contains("502")) return 502;
        if (errorMessage.contains("503")) return 503;
        if (errorMessage.contains("504")) return 504;

        return 0; // Unknown or network error
    }

    /**
     * Categoriza o tipo de falha
     */
    private String categorizeFailure(String errorMessage) {
        if (errorMessage == null) return "unknown";

        String message = errorMessage.toLowerCase();

        if (message.contains("timeout")) return "timeout";
        if (message.contains("connection") || message.contains("network")) return "network";
        if (message.contains("4")) return "client_error";
        if (message.contains("5")) return "server_error";

        return "unknown";
    }

    /**
     * Obtém circuit breaker para o endpoint
     */
    private CircuitBreaker getCircuitBreakerForEndpoint(String url) {
        String host = extractHostFromUrl(url);
        return circuitBreakerRegistry.circuitBreaker(host);
    }

    /**
     * Obtém rate limiter para o merchant
     */
    private RateLimiter getRateLimiterForMerchant(String merchantId) {
        return rateLimiterRegistry.rateLimiter(merchantId);
    }

    /**
     * Extrai host da URL para chave do circuit breaker
     */
    private String extractHostFromUrl(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return url; // Fallback to full URL
        }
    }

    /**
     * Job schedulado para processar webhooks com retry pendente
     */
    @Scheduled(fixedDelay = 60000) // A cada minuto
    public void processRetryQueue() {
        try {
            List<Webhook> pendingRetries = webhookRepository.findPendingRetries(Instant.now());

            if (!pendingRetries.isEmpty()) {
                log.info("Processing {} webhooks with pending retries", pendingRetries.size());

                pendingRetries.forEach(webhook -> {
                    try {
                        // Create retry event (this would normally come from stored webhook delivery data)
                        WebhookTriggeredEvent retryEvent = createRetryEventFromWebhook(webhook);
                        kafkaTemplate.send("webhook-events", webhook.getId(), retryEvent);

                        log.debug("Queued retry for webhook: webhookId={}", webhook.getId());

                    } catch (Exception e) {
                        log.error("Failed to queue retry for webhook: webhookId={}, error={}",
                                 webhook.getId(), e.getMessage(), e);
                    }
                });
            }

        } catch (Exception e) {
            log.error("Error processing retry queue: {}", e.getMessage(), e);
        }
    }

    /**
     * Cria evento de retry a partir do webhook
     */
    private WebhookTriggeredEvent createRetryEventFromWebhook(Webhook webhook) {
        return new WebhookTriggeredEvent(
                webhook.getId(),
                webhook.getMerchantId(),
                webhook.getUrl(),
                "payment.completed", // Default event type - would be stored with webhook
                "{}",  // Default payload - would be stored with webhook delivery
                Map.of(), // Default headers
                null,  // No signature for retry
                webhook.getRetryAttempts() + 1,
                webhookConfig.getMaxRetryAttempts(),
                UUID.randomUUID().toString(), // New correlation ID
                null // No causation ID for retry
        );
    }

    /**
     * Health check do worker de webhooks
     */
    public boolean isHealthy() {
        try {
            // Check if we can access the webhook repository
            long webhookCount = webhookRepository.count();

            // Check circuit breaker states
            long openCircuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers()
                    .stream()
                    .filter(cb -> cb.getState() == CircuitBreaker.State.OPEN)
                    .count();

            // Consider unhealthy if too many circuit breakers are open
            return openCircuitBreakers < 5; // Arbitrary threshold

        } catch (Exception e) {
            log.error("Webhook worker health check failed: {}", e.getMessage(), e);
            return false;
        }
    }
}