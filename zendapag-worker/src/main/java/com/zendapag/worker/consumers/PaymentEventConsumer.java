package com.zendapag.worker.consumers;

import com.zendapag.core.events.PaymentCreatedEvent;
import com.zendapag.core.events.PaymentCompletedEvent;
import com.zendapag.core.events.PaymentFailedEvent;
import com.zendapag.core.events.PaymentCancelledEvent;
import com.zendapag.worker.services.PaymentProcessingService;
import com.zendapag.worker.services.RiskAnalysisService;
import com.zendapag.worker.services.NotificationService;
import com.zendapag.worker.services.WebhookDeliveryService;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka consumer for payment-related events.
 * Processes payment lifecycle events with high throughput and reliability.
 */
@Component
@KafkaListener(
    topics = "payment-events",
    groupId = "payment-processor",
    containerFactory = "paymentEventsContainerFactory"
)
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PaymentProcessingService paymentProcessingService;
    private final RiskAnalysisService riskAnalysisService;
    private final NotificationService notificationService;
    private final WebhookDeliveryService webhookDeliveryService;

    // Metrics
    private final Counter eventCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;

    public PaymentEventConsumer(PaymentProcessingService paymentProcessingService,
                              RiskAnalysisService riskAnalysisService,
                              NotificationService notificationService,
                              WebhookDeliveryService webhookDeliveryService,
                              MeterRegistry meterRegistry) {
        this.paymentProcessingService = paymentProcessingService;
        this.riskAnalysisService = riskAnalysisService;
        this.notificationService = notificationService;
        this.webhookDeliveryService = webhookDeliveryService;

        this.eventCounter = Counter.builder("kafka.events.processed")
                .tag("topic", "payment-events")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("kafka.events.error")
                .tag("topic", "payment-events")
                .register(meterRegistry);
        this.processingTimer = Timer.builder("kafka.events.processing.time")
                .tag("topic", "payment-events")
                .register(meterRegistry);
    }

    @KafkaHandler
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handlePaymentCreated(@Payload PaymentCreatedEvent event,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        try {
            log.info("Processing PaymentCreatedEvent: paymentId={}, merchantId={}, amount={}, partition={}, offset={}",
                    event.getPaymentId(), event.getMerchantId(), event.getAmount(), partition, offset);

            // Process in parallel for better performance
            CompletableFuture<Void> riskAnalysis = CompletableFuture.runAsync(() ->
                processRiskAnalysis(event));

            CompletableFuture<Void> webhookDelivery = CompletableFuture.runAsync(() ->
                processWebhookDelivery(event));

            CompletableFuture<Void> notification = CompletableFuture.runAsync(() ->
                processNotification(event));

            // Wait for all async operations to complete
            CompletableFuture.allOf(riskAnalysis, webhookDelivery, notification).join();

            // Track metrics
            eventCounter.increment(
                "event_type", "payment_created",
                "merchant_id", event.getMerchantId(),
                "payment_method", event.getPaymentMethod().toString()
            );

            log.debug("Successfully processed PaymentCreatedEvent: paymentId={}", event.getPaymentId());

            // Manual acknowledgment
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process PaymentCreatedEvent: paymentId={}, error={}",
                     event.getPaymentId(), e.getMessage(), e);

            errorCounter.increment(
                "event_type", "payment_created",
                "error_type", e.getClass().getSimpleName()
            );

            throw e; // Re-throw to trigger retry mechanism
        } finally {
            sample.stop(processingTimer.tag("event_type", "payment_created"));
        }
    }

    @KafkaHandler
    @Transactional
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handlePaymentCompleted(@Payload PaymentCompletedEvent event,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        try {
            log.info("Processing PaymentCompletedEvent: paymentId={}, merchantId={}, amount={}, partition={}, offset={}",
                    event.getPaymentId(), event.getMerchantId(), event.getAmount(), partition, offset);

            // Update payment status
            paymentProcessingService.markPaymentAsCompleted(event);

            // Process settlement if required
            if (event.shouldTriggerSettlement()) {
                paymentProcessingService.initiateSettlement(event);
            }

            // Process in parallel
            CompletableFuture<Void> webhookDelivery = CompletableFuture.runAsync(() ->
                processWebhookDelivery(event));

            CompletableFuture<Void> notification = CompletableFuture.runAsync(() ->
                processCompletionNotification(event));

            // Wait for async operations
            CompletableFuture.allOf(webhookDelivery, notification).join();

            // Track metrics
            eventCounter.increment(
                "event_type", "payment_completed",
                "merchant_id", event.getMerchantId(),
                "payment_method", event.getPaymentMethod().toString(),
                "has_settlement", String.valueOf(event.shouldTriggerSettlement())
            );

            log.info("Successfully processed PaymentCompletedEvent: paymentId={}, netAmount={}",
                    event.getPaymentId(), event.getNetAmount());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process PaymentCompletedEvent: paymentId={}, error={}",
                     event.getPaymentId(), e.getMessage(), e);

            errorCounter.increment(
                "event_type", "payment_completed",
                "error_type", e.getClass().getSimpleName()
            );

            throw e;
        } finally {
            sample.stop(processingTimer.tag("event_type", "payment_completed"));
        }
    }

    @KafkaHandler
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handlePaymentFailed(@Payload PaymentFailedEvent event,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        try {
            log.info("Processing PaymentFailedEvent: paymentId={}, merchantId={}, reason={}, partition={}, offset={}",
                    event.getPaymentId(), event.getMerchantId(), event.getFailureReason(), partition, offset);

            // Update payment status
            paymentProcessingService.markPaymentAsFailed(event);

            // Process in parallel
            CompletableFuture<Void> webhookDelivery = CompletableFuture.runAsync(() ->
                processWebhookDelivery(event));

            CompletableFuture<Void> notification = CompletableFuture.runAsync(() ->
                processFailureNotification(event));

            CompletableFuture.allOf(webhookDelivery, notification).join();

            // Track metrics
            eventCounter.increment(
                "event_type", "payment_failed",
                "merchant_id", event.getMerchantId(),
                "failure_reason", event.getFailureReason() != null ? event.getFailureReason() : "unknown"
            );

            log.debug("Successfully processed PaymentFailedEvent: paymentId={}", event.getPaymentId());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process PaymentFailedEvent: paymentId={}, error={}",
                     event.getPaymentId(), e.getMessage(), e);

            errorCounter.increment(
                "event_type", "payment_failed",
                "error_type", e.getClass().getSimpleName()
            );

            throw e;
        } finally {
            sample.stop(processingTimer.tag("event_type", "payment_failed"));
        }
    }

    @KafkaHandler
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handlePaymentCancelled(@Payload PaymentCancelledEvent event,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        try {
            log.info("Processing PaymentCancelledEvent: paymentId={}, merchantId={}, reason={}, partition={}, offset={}",
                    event.getPaymentId(), event.getMerchantId(), event.getReason(), partition, offset);

            // Update payment status
            paymentProcessingService.markPaymentAsCancelled(event);

            // Process webhook delivery
            processWebhookDelivery(event);

            // Track metrics
            eventCounter.increment(
                "event_type", "payment_cancelled",
                "merchant_id", event.getMerchantId()
            );

            log.debug("Successfully processed PaymentCancelledEvent: paymentId={}", event.getPaymentId());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process PaymentCancelledEvent: paymentId={}, error={}",
                     event.getPaymentId(), e.getMessage(), e);

            errorCounter.increment(
                "event_type", "payment_cancelled",
                "error_type", e.getClass().getSimpleName()
            );

            throw e;
        } finally {
            sample.stop(processingTimer.tag("event_type", "payment_cancelled"));
        }
    }

    // Handler for unknown event types
    @KafkaHandler(isDefault = true)
    public void handleUnknownEvent(Object event, Acknowledgment acknowledgment) {
        log.warn("Received unknown event type: {}", event.getClass().getName());

        errorCounter.increment(
            "event_type", "unknown",
            "error_type", "UnknownEventType"
        );

        // Acknowledge to avoid infinite retries
        acknowledgment.acknowledge();
    }

    // Private helper methods
    private void processRiskAnalysis(PaymentCreatedEvent event) {
        try {
            riskAnalysisService.analyzePayment(
                event.getPaymentId(),
                event.getMerchantId(),
                event.getAmount(),
                event.getCustomerDocument(),
                event.getEventData()
            );
        } catch (Exception e) {
            log.warn("Risk analysis failed for payment {}: {}", event.getPaymentId(), e.getMessage());
            // Don't re-throw - risk analysis failure shouldn't block payment processing
        }
    }

    private void processWebhookDelivery(Object event) {
        try {
            webhookDeliveryService.deliverEvent(event);
        } catch (Exception e) {
            log.warn("Webhook delivery failed for event {}: {}", event.getClass().getSimpleName(), e.getMessage());
            // Don't re-throw - webhook failures are handled separately
        }
    }

    private void processNotification(PaymentCreatedEvent event) {
        try {
            notificationService.sendPaymentCreatedNotification(
                event.getMerchantId(),
                event.getPaymentId(),
                event.getAmount(),
                event.getCustomerEmail()
            );
        } catch (Exception e) {
            log.warn("Notification failed for payment {}: {}", event.getPaymentId(), e.getMessage());
            // Don't re-throw - notification failure shouldn't block payment processing
        }
    }

    private void processCompletionNotification(PaymentCompletedEvent event) {
        try {
            notificationService.sendPaymentCompletedNotification(
                event.getMerchantId(),
                event.getPaymentId(),
                event.getAmount(),
                event.getNetAmount()
            );
        } catch (Exception e) {
            log.warn("Completion notification failed for payment {}: {}", event.getPaymentId(), e.getMessage());
        }
    }

    private void processFailureNotification(PaymentFailedEvent event) {
        try {
            notificationService.sendPaymentFailedNotification(
                event.getMerchantId(),
                event.getPaymentId(),
                event.getFailureReason()
            );
        } catch (Exception e) {
            log.warn("Failure notification failed for payment {}: {}", event.getPaymentId(), e.getMessage());
        }
    }
}