package com.zendapag.core.pix.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.event.PaymentWebhookEvent;
import com.zendapag.core.event.PaymentStatusChangeEvent;
import com.zendapag.core.pix.config.PixConfig;
import com.zendapag.core.pix.dto.PixWebhookPayload;
import com.zendapag.core.pix.security.PixCertificateManager;
import com.zendapag.core.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PixWebhookProcessor {

    private final PixConfig pixConfig;
    private final PixCertificateManager certificateManager;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // In-memory idempotency store (in production, use Redis or database)
    private final Set<String> processedWebhooks = ConcurrentHashMap.newKeySet();

    @Autowired
    public PixWebhookProcessor(
            PixConfig pixConfig,
            PixCertificateManager certificateManager,
            ObjectMapper objectMapper,
            PaymentRepository paymentRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.pixConfig = pixConfig;
        this.certificateManager = certificateManager;
        this.objectMapper = objectMapper;
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Process incoming PIX webhook from participant
     */
    public WebhookProcessingResult processWebhook(String webhookPayload, String signature, String timestamp) {
        log.info("Processing PIX webhook with timestamp: {}", timestamp);

        try {
            // 1. Validate webhook payload structure
            PixWebhookPayload payload = validateAndParsePayload(webhookPayload);

            // 2. Check idempotency
            if (isAlreadyProcessed(payload.getWebhookId())) {
                log.info("Webhook already processed: {}", payload.getWebhookId());
                return WebhookProcessingResult.success("Already processed", payload.getWebhookId());
            }

            // 3. Validate signature
            if (!validateSignature(webhookPayload, signature, timestamp)) {
                log.error("Invalid webhook signature for webhook: {}", payload.getWebhookId());
                return WebhookProcessingResult.failure("Invalid signature", payload.getWebhookId());
            }

            // 4. Process webhook asynchronously via Kafka
            sendToKafkaProcessing(webhookPayload);

            // 5. Mark as received (not yet processed)
            markAsReceived(payload.getWebhookId());

            log.info("Webhook queued for processing: {}", payload.getWebhookId());
            return WebhookProcessingResult.success("Queued for processing", payload.getWebhookId());

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return WebhookProcessingResult.failure("Processing error: " + e.getMessage(), null);
        }
    }

    /**
     * Kafka consumer for processing webhooks asynchronously
     */
    @KafkaListener(topics = "#{@pixConfig.getWebhook().getKafkaTopicWebhooks()}", groupId = "pix-webhook-processor")
    @Transactional
    public void handleWebhookFromKafka(String message) {
        log.debug("Processing webhook from Kafka queue");

        try {
            PixWebhookPayload payload = objectMapper.readValue(message, PixWebhookPayload.class);
            processWebhookPayload(payload);
            markAsProcessed(payload.getWebhookId());

        } catch (Exception e) {
            log.error("Error processing webhook from Kafka: {}", e.getMessage(), e);
            handleWebhookProcessingFailure(message, e);
        }
    }

    @Async
    @Transactional
    public void processWebhookPayload(PixWebhookPayload payload) {
        log.info("Processing PIX webhook: {} for payment: {}", payload.getEventType(), payload.getTxId());

        try {
            // Find associated payment
            Payment payment = findPaymentByReference(payload.getTxId(), payload.getReferenceId());

            if (payment == null) {
                log.warn("Payment not found for webhook: {} txId: {}", payload.getWebhookId(), payload.getTxId());
                // Could be a webhook for external payment - handle accordingly
                return;
            }

            // Process based on event type
            PixWebhookPayload.EventType eventType = payload.getEventTypeEnum();

            switch (eventType) {
                case PIX_PAYMENT_COMPLETED:
                    handlePaymentCompleted(payment, payload);
                    break;

                case PIX_PAYMENT_EXPIRED:
                    handlePaymentExpired(payment, payload);
                    break;

                case PIX_PAYMENT_CANCELLED:
                    handlePaymentCancelled(payment, payload);
                    break;

                case PIX_PAYMENT_REJECTED:
                    handlePaymentRejected(payment, payload);
                    break;

                case PIX_PAYMENT_ERROR:
                    handlePaymentError(payment, payload);
                    break;

                default:
                    log.warn("Unknown webhook event type: {}", payload.getEventType());
            }

            // Publish internal event
            eventPublisher.publishEvent(new PaymentWebhookEvent(this, payment.getId(), payload));

            log.info("Successfully processed webhook: {}", payload.getWebhookId());

        } catch (Exception e) {
            log.error("Error processing webhook payload: {}", e.getMessage(), e);
            throw e; // Will trigger retry mechanism
        }
    }

    private void handlePaymentCompleted(Payment payment, PixWebhookPayload payload) {
        log.info("Handling completed payment: {}", payment.getReferenceId());

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            log.debug("Payment already completed: {}", payment.getReferenceId());
            return;
        }

        // Update payment details
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(payload.getPayment().getPaidAt());
        payment.setProcessedAt(payload.getPayment().getProcessedAt());

        // Set payer information if available
        if (payload.getPayer() != null) {
            payment.setPayerDocument(payload.getPayer().getDocument());
            payment.setPayerName(payload.getPayer().getName());

            if (payload.getPayer().getBank() != null) {
                payment.setPayerBank(payload.getPayer().getBank().getName());
                payment.setPayerBankIspb(payload.getPayer().getBank().getIspb());
            }
        }

        // Verify amount matches
        if (payload.getPayment().getAmount() != null &&
            payment.getAmount().compareTo(payload.getPayment().getAmount()) != 0) {
            log.warn("Amount mismatch in webhook. Expected: {}, Received: {}",
                    payment.getAmount(), payload.getPayment().getAmount());
        }

        paymentRepository.save(payment);

        // Publish payment completion event
        eventPublisher.publishEvent(new PaymentStatusChangeEvent(
            this, payment.getId(), Payment.PaymentStatus.COMPLETED, "PIX payment completed"
        ));
    }

    private void handlePaymentExpired(Payment payment, PixWebhookPayload payload) {
        log.info("Handling expired payment: {}", payment.getReferenceId());

        if (payment.getStatus() == Payment.PaymentStatus.EXPIRED) {
            log.debug("Payment already expired: {}", payment.getReferenceId());
            return;
        }

        payment.setStatus(Payment.PaymentStatus.EXPIRED);
        payment.setProcessedAt(Instant.now());
        paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentStatusChangeEvent(
            this, payment.getId(), Payment.PaymentStatus.EXPIRED, "PIX payment expired"
        ));
    }

    private void handlePaymentCancelled(Payment payment, PixWebhookPayload payload) {
        log.info("Handling cancelled payment: {}", payment.getReferenceId());

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        payment.setProcessedAt(Instant.now());

        if (payload.getPayment().getStatusReason() != null) {
            payment.setCancellationReason(payload.getPayment().getStatusReason());
        }

        paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentStatusChangeEvent(
            this, payment.getId(), Payment.PaymentStatus.CANCELLED, "PIX payment cancelled"
        ));
    }

    private void handlePaymentRejected(Payment payment, PixWebhookPayload payload) {
        log.info("Handling rejected payment: {}", payment.getReferenceId());

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setProcessedAt(Instant.now());

        if (payload.getPayment().getStatusReason() != null) {
            payment.setFailureReason(payload.getPayment().getStatusReason());
        }

        paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentStatusChangeEvent(
            this, payment.getId(), Payment.PaymentStatus.FAILED, "PIX payment rejected"
        ));
    }

    private void handlePaymentError(Payment payment, PixWebhookPayload payload) {
        log.error("Handling payment error: {}", payment.getReferenceId());

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setProcessedAt(Instant.now());

        if (payload.getPayment().getStatusReason() != null) {
            payment.setFailureReason(payload.getPayment().getStatusReason());
        }

        paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentStatusChangeEvent(
            this, payment.getId(), Payment.PaymentStatus.FAILED, "PIX payment error"
        ));
    }

    private PixWebhookPayload validateAndParsePayload(String webhookPayload) throws Exception {
        if (webhookPayload == null || webhookPayload.trim().isEmpty()) {
            throw new WebhookValidationException("Empty webhook payload");
        }

        try {
            PixWebhookPayload payload = objectMapper.readValue(webhookPayload, PixWebhookPayload.class);

            // Basic validation
            if (payload.getWebhookId() == null || payload.getWebhookId().trim().isEmpty()) {
                throw new WebhookValidationException("Missing webhook ID");
            }

            if (payload.getEventType() == null || payload.getEventType().trim().isEmpty()) {
                throw new WebhookValidationException("Missing event type");
            }

            if (payload.getTxId() == null || payload.getTxId().trim().isEmpty()) {
                throw new WebhookValidationException("Missing transaction ID");
            }

            return payload;

        } catch (Exception e) {
            log.error("Invalid webhook payload format: {}", e.getMessage());
            throw new WebhookValidationException("Invalid JSON payload", e);
        }
    }

    private boolean validateSignature(String payload, String signature, String timestamp) {
        try {
            // Validate timestamp (prevent replay attacks)
            if (!isValidTimestamp(timestamp)) {
                log.warn("Invalid or old timestamp in webhook: {}", timestamp);
                return false;
            }

            // Validate signature
            String webhookSecret = pixConfig.getWebhook().getSecret();
            return certificateManager.validateWebhookSignature(payload, signature, webhookSecret);

        } catch (Exception e) {
            log.error("Error validating webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean isValidTimestamp(String timestamp) {
        try {
            Instant webhookTime = Instant.parse(timestamp);
            Instant now = Instant.now();
            Duration age = Duration.between(webhookTime, now);

            // Accept webhooks up to 5 minutes old
            return age.toMinutes() <= 5 && !webhookTime.isAfter(now.plusSeconds(30));

        } catch (Exception e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }

    private boolean isAlreadyProcessed(String webhookId) {
        return processedWebhooks.contains(webhookId);
    }

    private void markAsReceived(String webhookId) {
        // In production, store in Redis with TTL
        log.debug("Marking webhook as received: {}", webhookId);
    }

    private void markAsProcessed(String webhookId) {
        processedWebhooks.add(webhookId);
        log.debug("Marking webhook as processed: {}", webhookId);
    }

    private void sendToKafkaProcessing(String webhookPayload) {
        try {
            String topic = pixConfig.getWebhook().getKafkaTopicWebhooks();
            kafkaTemplate.send(topic, webhookPayload);
            log.debug("Webhook sent to Kafka topic: {}", topic);

        } catch (Exception e) {
            log.error("Failed to send webhook to Kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to queue webhook for processing", e);
        }
    }

    private Payment findPaymentByReference(String txId, String referenceId) {
        // Try to find by txId first
        if (txId != null) {
            Payment payment = paymentRepository.findByPixTxId(txId).orElse(null);
            if (payment != null) {
                return payment;
            }
        }

        // Fallback to referenceId
        if (referenceId != null) {
            return paymentRepository.findByReferenceId(referenceId).orElse(null);
        }

        return null;
    }

    private void handleWebhookProcessingFailure(String message, Exception e) {
        log.error("Webhook processing failed, sending to dead letter queue: {}", e.getMessage());

        try {
            // Send to dead letter topic
            String deadLetterTopic = pixConfig.getWebhook().getKafkaTopicWebhooks() + "-dlq";
            kafkaTemplate.send(deadLetterTopic, message);

        } catch (Exception dlqError) {
            log.error("Failed to send to dead letter queue: {}", dlqError.getMessage(), dlqError);
        }
    }

    // Result classes
    public static class WebhookProcessingResult {
        private final boolean success;
        private final String message;
        private final String webhookId;

        private WebhookProcessingResult(boolean success, String message, String webhookId) {
            this.success = success;
            this.message = message;
            this.webhookId = webhookId;
        }

        public static WebhookProcessingResult success(String message, String webhookId) {
            return new WebhookProcessingResult(true, message, webhookId);
        }

        public static WebhookProcessingResult failure(String message, String webhookId) {
            return new WebhookProcessingResult(false, message, webhookId);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getWebhookId() { return webhookId; }
    }

    // Custom exception
    public static class WebhookValidationException extends RuntimeException {
        public WebhookValidationException(String message) {
            super(message);
        }

        public WebhookValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}