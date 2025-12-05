package com.zendapag.core.pix.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.pix.config.PixConfig;
import com.zendapag.core.pix.dto.PixWebhookPayload;
import com.zendapag.core.pix.security.PixCertificateManager;
import com.zendapag.core.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"staging", "prod"})
@Slf4j
public class PixWebhookProcessor {

    private final PixConfig pixConfig;
    private final PixCertificateManager certificateManager;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final Set<String> processedWebhooks = ConcurrentHashMap.newKeySet();

    @Autowired
    public PixWebhookProcessor(PixConfig pixConfig, PixCertificateManager certificateManager, ObjectMapper objectMapper, PaymentRepository paymentRepository, KafkaTemplate<String, String> kafkaTemplate, ApplicationEventPublisher eventPublisher) {
        this.pixConfig = pixConfig;
        this.certificateManager = certificateManager;
        this.objectMapper = objectMapper;
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.eventPublisher = eventPublisher;
    }

    public WebhookProcessingResult processWebhook(String webhookPayload, String signature, String timestamp) {
        log.info("Processing PIX webhook with timestamp: {}", timestamp);
        try {
            PixWebhookPayload payload = objectMapper.readValue(webhookPayload, PixWebhookPayload.class);
            if (processedWebhooks.contains(payload.getWebhookId())) {
                return WebhookProcessingResult.success("Already processed", payload.getWebhookId());
            }
            processedWebhooks.add(payload.getWebhookId());
            return WebhookProcessingResult.success("Processed", payload.getWebhookId());
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return WebhookProcessingResult.failure("Processing error: " + e.getMessage(), null);
        }
    }

    @Transactional
    public void processWebhookPayload(PixWebhookPayload payload) {
        log.info("Processing PIX webhook payload: {}", payload.getEventType());
        Payment payment = paymentRepository.findByPixTxId(payload.getTxId()).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for webhook txId: {}", payload.getTxId());
            return;
        }
        switch (payload.getEventTypeEnum()) {
            case PIX_PAYMENT_COMPLETED -> { payment.setStatus(PaymentStatus.APPROVED); payment.setPaidAt(Instant.now()); }
            case PIX_PAYMENT_EXPIRED -> payment.setStatus(PaymentStatus.EXPIRED);
            case PIX_PAYMENT_CANCELLED -> payment.setStatus(PaymentStatus.CANCELLED);
            case PIX_PAYMENT_REJECTED, PIX_PAYMENT_ERROR -> payment.setStatus(PaymentStatus.REJECTED);
            default -> log.warn("Unknown webhook event type: {}", payload.getEventType());
        }
        paymentRepository.save(payment);
    }

    public static class WebhookProcessingResult {
        private final boolean success;
        private final String message;
        private final String webhookId;

        private WebhookProcessingResult(boolean success, String message, String webhookId) {
            this.success = success; this.message = message; this.webhookId = webhookId;
        }
        public static WebhookProcessingResult success(String message, String webhookId) { return new WebhookProcessingResult(true, message, webhookId); }
        public static WebhookProcessingResult failure(String message, String webhookId) { return new WebhookProcessingResult(false, message, webhookId); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getWebhookId() { return webhookId; }
    }

    public static class WebhookValidationException extends RuntimeException {
        public WebhookValidationException(String message) { super(message); }
        public WebhookValidationException(String message, Throwable cause) { super(message, cause); }
    }
}
