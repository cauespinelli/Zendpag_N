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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    private final MeterRegistry meterRegistry;

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
        this.meterRegistry = meterRegistry;

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
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handlePaymentCreated(@Payload PaymentCreatedEvent event,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment acknowledgment) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Processing PaymentCreatedEvent: paymentId={}, merchantId={}", event.getPaymentId(), event.getMerchantId());
            processRiskAnalysis(event);
            processWebhookDelivery(event.getPaymentId(), "payment_created");
            processNotification(event);
            eventCounter.increment();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process PaymentCreatedEvent", e);
            errorCounter.increment();
            throw e;
        } finally {
            sample.stop(processingTimer);
        }
    }

    @KafkaHandler
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handlePaymentCompleted(@Payload PaymentCompletedEvent event,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     Acknowledgment acknowledgment) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Processing PaymentCompletedEvent: paymentId={}", event.getPaymentId());
            paymentProcessingService.markPaymentAsCompleted(event);
            if (event.shouldTriggerSettlement()) {
                paymentProcessingService.initiateSettlement(event);
            }
            processWebhookDelivery(event.getPaymentId(), "payment_completed");
            processCompletionNotification(event);
            eventCounter.increment();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process PaymentCompletedEvent", e);
            errorCounter.increment();
            throw e;
        } finally {
            sample.stop(processingTimer);
        }
    }

    @KafkaHandler
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handlePaymentFailed(@Payload PaymentFailedEvent event,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Processing PaymentFailedEvent: paymentId={}, error={}", event.getPaymentId(), event.getErrorMessage());
            paymentProcessingService.markPaymentAsFailed(event);
            processWebhookDelivery(event.getPaymentId(), "payment_failed");
            processFailureNotification(event);
            eventCounter.increment();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process PaymentFailedEvent", e);
            errorCounter.increment();
            throw e;
        } finally {
            sample.stop(processingTimer);
        }
    }

    @KafkaHandler
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handlePaymentCancelled(@Payload PaymentCancelledEvent event,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     Acknowledgment acknowledgment) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Processing PaymentCancelledEvent: paymentId={}", event.getPaymentId());
            paymentProcessingService.markPaymentAsCancelled(event);
            processWebhookDelivery(event.getPaymentId(), "payment_cancelled");
            eventCounter.increment();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process PaymentCancelledEvent", e);
            errorCounter.increment();
            throw e;
        } finally {
            sample.stop(processingTimer);
        }
    }

    @KafkaHandler(isDefault = true)
    public void handleUnknownEvent(Object event, Acknowledgment acknowledgment) {
        log.warn("Received unknown event type: {}", event.getClass().getName());
        errorCounter.increment();
        acknowledgment.acknowledge();
    }

    private void processRiskAnalysis(PaymentCreatedEvent event) {
        try {
            UUID paymentUUID = UUID.fromString(event.getPaymentId());
            riskAnalysisService.analyzePayment(paymentUUID, event.getAmount(), event.getCustomerDocument());
        } catch (Exception e) {
            log.warn("Risk analysis failed for payment {}: {}", event.getPaymentId(), e.getMessage());
        }
    }

    private void processWebhookDelivery(String paymentId, String eventType) {
        try {
            webhookDeliveryService.deliverWebhook(paymentId, eventType);
        } catch (Exception e) {
            log.warn("Webhook delivery failed for payment {}: {}", paymentId, e.getMessage());
        }
    }

    private void processNotification(PaymentCreatedEvent event) {
        try {
            UUID paymentUUID = UUID.fromString(event.getPaymentId());
            notificationService.notifyPaymentReceived(paymentUUID, event.getCustomerEmail());
        } catch (Exception e) {
            log.warn("Notification failed for payment {}: {}", event.getPaymentId(), e.getMessage());
        }
    }

    private void processCompletionNotification(PaymentCompletedEvent event) {
        try {
            UUID paymentUUID = UUID.fromString(event.getPaymentId());
            notificationService.notifyPaymentCompleted(paymentUUID, null);
        } catch (Exception e) {
            log.warn("Completion notification failed for payment {}: {}", event.getPaymentId(), e.getMessage());
        }
    }

    private void processFailureNotification(PaymentFailedEvent event) {
        try {
            UUID merchantUUID = UUID.fromString(event.getMerchantId());
            notificationService.notifyMerchant(merchantUUID, "Payment Failed", event.getErrorMessage());
        } catch (Exception e) {
            log.warn("Failure notification failed for payment {}: {}", event.getPaymentId(), e.getMessage());
        }
    }
}
