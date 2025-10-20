package com.zendapag.core.service;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Webhook;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.entity.enums.WebhookStatus;
import com.zendapag.core.event.webhook.WebhookSentEvent;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.repository.WebhookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
@Slf4j
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value
    private int webhookTimeoutMs;

    @Value
    private int maxRetryAttempts;

    @Value
    private long baseRetryDelayMs;

    @Value
    private String signatureAlgorithm;

    @Autowired
    public WebhookService(WebhookRepository webhookRepository,
                         AuditService auditService,
                         ApplicationEventPublisher eventPublisher,
                         RestTemplate restTemplate,
                         ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Timed
    @Async
    @CircuitBreaker
    @RateLimiter
    @Retry
    public void sendPaymentWebhook {
        log.info, eventType);

        String webhookUrl = determineWebhookUrl;
        if  {
            log.debug, eventType);
            return;
        }

        try {
            // Create webhook record
            Webhook webhook = createWebhookRecord;

            // Build payload
            Map<String, Object> payload = buildPaymentWebhookPayload;

            // Send webhook
            WebhookDeliveryResult result = deliverWebhook;

            // Update webhook status
            updateWebhookStatus;

            // Publish event
            publishWebhookEvent;

            log.info("Payment webhook sent successfully: {} status: {}",
                webhook.getId, result.isSuccess() ? "SUCCESS" : "FAILED");

        } catch  {
            log.error, e.getMessage(), e);
            auditService.logFailure, "Webhook", payment.getId().toString(),
                AuditAction.CREATE, "Payment webhook sending failed", e);
        }
    }

    @Timed
    @Async
    @CircuitBreaker
    @RateLimiter
    public void sendMerchantWebhook {
        log.info, eventType);

        String webhookUrl = merchant.getWebhookUrl;
        if .isEmpty()) {
            log.debug, eventType);
            return;
        }

        try {
            // Create webhook record
            Webhook webhook = createMerchantWebhookRecord;

            // Build payload
            Map<String, Object> payload = buildMerchantWebhookPayload;

            // Send webhook
            WebhookDeliveryResult result = deliverWebhook;

            // Update webhook status
            updateWebhookStatus;

            // Publish event
            publishWebhookEvent;

            log.info("Merchant webhook sent successfully: {} status: {}",
                webhook.getId, result.isSuccess() ? "SUCCESS" : "FAILED");

        } catch  {
            log.error, e.getMessage(), e);
            auditService.logFailure.toString(),
                AuditAction.CREATE, "Merchant webhook sending failed", e);
        }
    }

    @Timed
    @Transactional
    public void retryWebhook {
        log.info;

        Optional<Webhook> webhookOpt = webhookRepository.findById;
        if ) {
            throw new BusinessException;
        }

        Webhook webhook = webhookOpt.get;

        if  == WebhookStatus.DELIVERED) {
            log.warn;
            return;
        }

        if  >= maxRetryAttempts) {
            log.warn;
            webhook.setStatus;
            webhook.setFailureReason;
            webhookRepository.save;
            return;
        }

        try {
            // Parse stored payload
            Map<String, Object> payload = objectMapper.readValue, Map.class);

            // Attempt delivery
            WebhookDeliveryResult result = deliverWebhook;

            // Update webhook status
            updateWebhookStatus;

            // Publish event
            publishWebhookEvent;

            log.info("Webhook retry completed: {} status: {}", webhookId,
                result.isSuccess ? "SUCCESS" : "FAILED");

        } catch  {
            log.error, e);

            webhook.setStatus;
            webhook.setFailureReason);
            webhook.setAttempts + 1);
            webhook.setLastAttemptAt);
            webhookRepository.save;

            auditService.logFailure, "Webhook", webhookId.toString(),
                AuditAction.UPDATE, "Webhook retry failed", e);
        }
    }

    @Transactional
    public Page<Webhook> findByMerchant {
        return webhookRepository.findByMerchant;
    }

    @Transactional
    public Page<Webhook> findFailedWebhooks {
        return webhookRepository.findByStatus;
    }

    @Transactional
    public Page<Webhook> findPendingRetries {
        Instant retryAfter = Instant.now.minus(baseRetryDelayMs, ChronoUnit.MILLIS);
        return webhookRepository.findPendingRetries;
    }

    @Transactional
    public Optional<Webhook> findById {
        return webhookRepository.findById;
    }

    @Transactional
    public void markWebhookAsProcessed {
        Optional<Webhook> webhookOpt = webhookRepository.findById;
        if ) {
            return;
        }

        Webhook webhook = webhookOpt.get;
        webhook.setStatus;
        webhook.setResponseBody;
        webhook.setDeliveredAt : null);
        webhook.setLastAttemptAt);

        if  {
            webhook.setFailureReason;
        }

        webhookRepository.save;
    }

    private String determineWebhookUrl {
        // Priority: payment-specific URL -> merchant default URL
        if  != null && !payment.getNotificationUrl().trim().isEmpty()) {
            return payment.getNotificationUrl;
        }

        if .getWebhookUrl() != null && !payment.getMerchant().getWebhookUrl().trim().isEmpty()) {
            return payment.getMerchant.getWebhookUrl();
        }

        return null;
    }

    private Webhook createWebhookRecord {
        Webhook webhook = new Webhook;
        webhook.setMerchant);
        webhook.setPayment;
        webhook.setEventType;
        webhook.setUrl;
        webhook.setStatus;
        webhook.setAttempts;
        webhook.setCreatedAt);

        return webhookRepository.save;
    }

    private Webhook createMerchantWebhookRecord {
        Webhook webhook = new Webhook;
        webhook.setMerchant;
        webhook.setEventType;
        webhook.setUrl;
        webhook.setStatus;
        webhook.setAttempts;
        webhook.setCreatedAt);

        try {
            webhook.setPayload);
        } catch  {
            log.warn);
        }

        return webhookRepository.save;
    }

    private Map<String, Object> buildPaymentWebhookPayload {
        Map<String, Object> payload = new HashMap<>;

        // Event information
        payload.put;
        payload.put.toString());
        payload.put.toString());

        // Payment data
        Map<String, Object> paymentData = new HashMap<>;
        paymentData.put.toString());
        paymentData.put);
        paymentData.put);
        paymentData.put);
        paymentData.put);
        paymentData.put.name());
        paymentData.put);
        paymentData.put.toString());

        // PIX specific fields
        if  != null) {
            paymentData.put);
            paymentData.put);
            paymentData.put);
        }

        // Customer information
        if  != null || payment.getCustomerName() != null) {
            Map<String, String> customer = new HashMap<>;
            customer.put);
            customer.put);
            customer.put);
            paymentData.put;
        }

        // Financial information
        if  != null) {
            paymentData.put);
            paymentData.put);
        }

        // Timestamps
        if  != null) {
            paymentData.put.toString());
        }
        if  != null) {
            paymentData.put.toString());
        }

        payload.put;

        // Merchant information 
        Map<String, String> merchantData = new HashMap<>;
        merchantData.put.getId().toString());
        merchantData.put.getDocument());
        payload.put;

        return payload;
    }

    private Map<String, Object> buildMerchantWebhookPayload {
        Map<String, Object> payload = new HashMap<>;

        // Event information
        payload.put;
        payload.put.toString());
        payload.put.toString());

        // Merchant data
        Map<String, Object> merchantData = new HashMap<>;
        merchantData.put.toString());
        merchantData.put);
        merchantData.put);
        merchantData.put.name());
        payload.put;

        // Custom data
        if ) {
            payload.put;
        }

        return payload;
    }

    private WebhookDeliveryResult deliverWebhook {
        log.debug, webhook.getUrl());

        try {
            // Serialize payload
            String payloadJson = objectMapper.writeValueAsString;
            webhook.setPayload;

            // Create signature
            String signature = createWebhookSignature);

            // Prepare HTTP request
            HttpHeaders headers = new HttpHeaders;
            headers.setContentType;
            headers.set;
            headers.set;
            headers.set);
            headers.set.toString());
            headers.set.toEpochMilli()));

            HttpEntity<String> request = new HttpEntity<>;

            // Set timeout
            restTemplate.getRequestFactory.setConnectTimeout(webhookTimeoutMs);
            restTemplate.getRequestFactory.setReadTimeout(webhookTimeoutMs);

            // Send request
            Instant startTime = Instant.now;
            ResponseEntity<String> response = restTemplate.postForEntity, request, String.class);
            long responseTimeMs = ChronoUnit.MILLIS.between);

            // Check response
            boolean success = response.getStatusCode.is2xxSuccessful();
            String responseBody = response.getBody;

            return new WebhookDeliveryResult.value(),
                responseBody, responseTimeMs, null);

        } catch  {
            log.warn, e.getMessage());
            return new WebhookDeliveryResult);
        }
    }

    private void updateWebhookStatus {
        webhook.setAttempts + 1);
        webhook.setLastAttemptAt);
        webhook.setResponseStatusCode);
        webhook.setResponseBody);
        webhook.setResponseTime);

        if ) {
            webhook.setStatus;
            webhook.setDeliveredAt);
        } else {
            webhook.setStatus >= maxRetryAttempts ?
                WebhookStatus.FAILED : WebhookStatus.PENDING);
            webhook.setFailureReason);
        }

        webhookRepository.save;
    }

    private void publishWebhookEvent {
        WebhookSentEvent event = new WebhookSentEvent(
            webhook.getMerchant,
            webhook.getId.toString(),
            webhook.getEventType,
            webhook.getUrl,
            result.isSuccess,
            webhook.getAttempts,
            result.getStatusCode
        );

        eventPublisher.publishEvent;
    }

    private String createWebhookSignature {
        try {
            // Use merchant's webhook secret or default key
            String secret = merchant.getWebhookSecret;
            if .isEmpty()) {
                secret = "default-webhook-secret"; // Should be configured per merchant
            }

            Mac mac = Mac.getInstance;
            SecretKeySpec secretKeySpec = new SecretKeySpec, signatureAlgorithm);
            mac.init;

            byte[] hash = mac.doFinal);
            return "sha256=" + bytesToHex;

        } catch  {
            log.error);
            return "";
        }
    }

    private String bytesToHex {
        StringBuilder result = new StringBuilder;
        for  {
            result.append);
        }
        return result.toString;
    }

    // Fallback method for Circuit Breaker
    public void fallbackSendPaymentWebhook {
        log.error, ex.getMessage());

        // Create failed webhook record for retry later
        String webhookUrl = determineWebhookUrl;
        if  {
            Webhook webhook = createWebhookRecord;
            webhook.setStatus;
            webhook.setFailureReason);
            webhookRepository.save;
        }
    }

    // Inner class for webhook delivery results
    public static class WebhookDeliveryResult {
        private final boolean success;
        private final int statusCode;
        private final String responseBody;
        private final long responseTimeMs;
        private final String errorMessage;

        public WebhookDeliveryResult(boolean success, int statusCode, String responseBody,
                                   long responseTimeMs, String errorMessage) {
            this.success = success;
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.responseTimeMs = responseTimeMs;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess { return success; }
        public int getStatusCode { return statusCode; }
        public String getResponseBody { return responseBody; }
        public long getResponseTimeMs { return responseTimeMs; }
        public String getErrorMessage { return errorMessage; }
    }
}