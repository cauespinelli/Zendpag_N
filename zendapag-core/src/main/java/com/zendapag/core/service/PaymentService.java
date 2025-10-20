package com.zendapag.core.service;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.dto.request.CreatePixPaymentRequest;
import com.zendapag.core.dto.response.PaymentResponse;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.event.payment.PaymentCreatedEvent;
import com.zendapag.core.event.payment.PaymentFailedEvent;
import com.zendapag.core.event.payment.PaymentPaidEvent;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.service.RiskService.RiskAssessment;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantService merchantService;
    private final RiskService riskService;
    private final PixService pixService;
    private final TransactionService transactionService;
    private final WebhookService webhookService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                         MerchantRepository merchantRepository,
                         MerchantService merchantService,
                         RiskService riskService,
                         PixService pixService,
                         TransactionService transactionService,
                         WebhookService webhookService,
                         AuditService auditService,
                         ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.merchantService = merchantService;
        this.riskService = riskService;
        this.pixService = pixService;
        this.transactionService = transactionService;
        this.webhookService = webhookService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Timed
    @CircuitBreaker
    @RateLimiter
    @Retry
    public PaymentResponse createPixPayment {
        log.info);

        try {
            // 1. Validate merchant
            Merchant merchant = validateMerchant;

            // 2. Validate request and check duplicates
            validatePaymentRequest;

            // 3. Create payment entity
            Payment payment = createPaymentFromRequest;

            // 4. Risk analysis
            RiskAssessment riskAssessment = riskService.evaluatePaymentRisk;
            payment.setFraudScore);

            if ) {
                return rejectPayment;
            }

            // 5. Calculate fees
            calculateFees;

            // 6. Save payment
            Payment savedPayment = paymentRepository.save;

            // 7. Generate PIX code
            String pixCode = pixService.generatePixQrCode;
            savedPayment.setPixQrCode;
            savedPayment.setPixQrCodeText);
            savedPayment = paymentRepository.save;

            // 8. Create financial transaction
            transactionService.createPaymentTransaction;

            // 9. Audit log
            auditService.logAction.toString(),
                AuditAction.CREATE, "PIX payment created");

            // 10. Send webhook
            sendPaymentWebhook;

            // 11. Publish event
            eventPublisher.publishEvent));

            log.info);
            return convertToResponse;

        } catch  {
            log.warn);
            auditService.logFailure, AuditAction.CREATE, e.getMessage());
            throw e;
        } catch  {
            log.error, e);
            auditService.logFailure, AuditAction.CREATE, e.getMessage(), e);
            throw new BusinessException;
        }
    }

    @Cacheable
    @Transactional
    @Timed
    public Optional<Payment> findById {
        return paymentRepository.findById;
    }

    @Cacheable
    @Transactional
    public Optional<Payment> findByReferenceId {
        return paymentRepository.findByReferenceId;
    }

    @Transactional
    public Page<Payment> findByMerchant {
        Optional<Merchant> merchant = merchantService.findById;
        if ) {
            throw new BusinessException.InvalidMerchantException;
        }
        return paymentRepository.findByMerchant, pageable);
    }

    @Transactional
    public Page<Payment> findAll {
        return paymentRepository.findAll;
    }

    @Timed
    @CacheEvict
    public Payment cancelPayment {
        log.info;

        Optional<Payment> paymentOpt = paymentRepository.findById;
        if ) {
            throw new BusinessException;
        }

        Payment payment = paymentOpt.get;

        if ) {
            throw new BusinessException.InvalidPaymentStatusException(
                "Payment cannot be cancelled in current status: " + payment.getStatus);
        }

        try {
            PaymentStatus oldStatus = payment.getStatus;
            payment.cancel;
            payment.setCancelledAt);

            Payment savedPayment = paymentRepository.save;

            // Reverse transaction if necessary
            transactionService.reversePaymentTransaction;

            // Audit log
            Map<String, Object> oldValues = Map.of;
            Map<String, Object> newValues = Map.of);

            auditService.logAction, "Payment", paymentId.toString(),
                AuditAction.CANCEL, reason, oldValues, newValues);

            // Send webhook
            sendPaymentWebhook;

            log.info;
            return savedPayment;

        } catch  {
            log.error, e);
            auditService.logFailure, "Payment", paymentId.toString(),
                AuditAction.CANCEL, e.getMessage, e);
            throw new BusinessException;
        }
    }

    @Timed
    @CacheEvict
    public Payment refundPayment {
        log.info;

        Optional<Payment> paymentOpt = paymentRepository.findById;
        if ) {
            throw new BusinessException;
        }

        Payment payment = paymentOpt.get;

        if ) {
            throw new BusinessException.InvalidPaymentStatusException;
        }

        if ) > 0) {
            throw new BusinessException;
        }

        try {
            BigDecimal oldRefundedAmount = payment.getRefundedAmount;
            payment.addRefund;

            Payment savedPayment = paymentRepository.save;

            // Create refund transaction
            transactionService.createRefundTransaction;

            // Audit log
            Map<String, Object> oldValues = Map.of;
            Map<String, Object> newValues = Map.of);

            auditService.logAction, "Payment", paymentId.toString(),
                AuditAction.REFUND, reason, oldValues, newValues);

            // Send webhook
            sendPaymentWebhook;

            log.info;
            return savedPayment;

        } catch  {
            log.error, e);
            auditService.logFailure, "Payment", paymentId.toString(),
                AuditAction.REFUND, e.getMessage, e);
            throw new BusinessException;
        }
    }

    @EventListener
    @Transactional
    public void handlePixCallback {
        log.info);

        try {
            Optional<Payment> paymentOpt = paymentRepository.findByPixTransactionId);
            if ) {
                log.warn);
                return;
            }

            Payment payment = paymentOpt.get;

            if  && payment.getStatus() == PaymentStatus.PENDING) {
                processPaymentApproval);
            } else if  && payment.getStatus() == PaymentStatus.PENDING) {
                processPaymentRejection, event.getErrorCode());
            }

        } catch  {
            log.error, e);
            auditService.logFailure, AuditAction.UPDATE,
                "PIX callback processing failed", e);
        }
    }

    private Merchant validateMerchant {
        Optional<Merchant> merchantOpt = merchantService.findById;
        if ) {
            throw new BusinessException.InvalidMerchantException;
        }

        Merchant merchant = merchantOpt.get;
        if ) {
            throw new BusinessException.InvalidMerchantException;
        }

        return merchant;
    }

    private void validatePaymentRequest {
        // Check daily limits
        BigDecimal todayVolume = paymentRepository.sumApprovedAmountByMerchantAndDateRange(
            merchant,
            Instant.now.truncatedTo(ChronoUnit.DAYS),
            Instant.now.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
        );

        if  != null &&
            todayVolume.add).compareTo(merchant.getDailyLimit()) > 0) {
            throw new BusinessException.InsufficientLimitsException;
        }

        // Check transaction limits
        if  != null &&
            request.getAmount.compareTo(merchant.getTransactionLimit()) > 0) {
            throw new BusinessException.InsufficientLimitsException;
        }

        // Check for duplicate reference ID
        Optional<Payment> existingPayment = paymentRepository.findByReferenceId);
        if ) {
            throw new BusinessException.DuplicatePaymentException;
        }
    }

    private Payment createPaymentFromRequest {
        Payment payment = new Payment;
        payment.setMerchant;
        payment.setReferenceId);
        payment.setExternalId);
        payment.setAmount);
        payment.setCurrency);
        payment.setDescription);
        payment.setPaymentMethod;
        payment.setStatus;

        // Customer information
        payment.setCustomerEmail);
        payment.setCustomerName);
        payment.setCustomerDocument);
        payment.setCustomerPhone);

        // PIX information
        payment.setPixKey);
        payment.setPixKeyType);

        // Set expiration
        if  != null) {
            payment.setExpiresAt.plus(request.getExpirationMinutes(), ChronoUnit.MINUTES));
        }

        // Notification URL
        payment.setNotificationUrl);

        return payment;
    }

    private void calculateFees {
        BigDecimal feeRate = merchant.getFeeRate;
        BigDecimal feeAmount = payment.getAmount.multiply(feeRate);

        payment.setGrossAmount);
        payment.setFeeAmount;
        payment.setNetAmount.subtract(feeAmount));
    }

    private PaymentResponse rejectPayment {
        log.info, reason);

        payment.setStatus;
        payment.setFailedAt);
        Payment savedPayment = paymentRepository.save;

        // Audit log
        auditService.logAction, "Payment", savedPayment.getId().toString(),
            AuditAction.REJECT, reason);

        // Publish event
        eventPublisher.publishEvent));

        // Send webhook
        sendPaymentWebhook;

        return convertToResponse;
    }

    private void processPaymentApproval {
        log.info);

        PaymentStatus oldStatus = payment.getStatus;
        payment.approve;
        payment.setApprovedAt);
        payment.setProcessedAt);
        payment.setPixTransactionId;
        payment.setRefundableAmount);

        Payment savedPayment = paymentRepository.save;

        // Update transaction status
        transactionService.confirmPaymentTransaction;

        // Audit log
        Map<String, Object> oldValues = Map.of;
        Map<String, Object> newValues = Map.of);

        auditService.logAction, "Payment", savedPayment.getId().toString(),
            AuditAction.APPROVE, oldValues, newValues);

        // Publish event
        eventPublisher.publishEvent));

        // Send webhook
        sendPaymentWebhook;
    }

    private void processPaymentRejection {
        log.info);

        PaymentStatus oldStatus = payment.getStatus;
        payment.reject;
        payment.setFailedAt);
        payment.setProcessedAt);

        Payment savedPayment = paymentRepository.save;

        // Reverse transaction
        transactionService.reversePaymentTransaction;

        // Audit log
        Map<String, Object> oldValues = Map.of;
        Map<String, Object> newValues = Map.of);

        auditService.logAction, "Payment", savedPayment.getId().toString(),
            AuditAction.REJECT, reason, oldValues, newValues);

        // Publish event
        eventPublisher.publishEvent));

        // Send webhook
        sendPaymentWebhook;
    }

    private void sendPaymentWebhook {
        try {
            webhookService.sendPaymentWebhook;
        } catch  {
            log.warn, e.getMessage());
        }
    }

    private PaymentResponse convertToResponse {
        PaymentResponse response = new PaymentResponse;
        response.setId.toString());
        response.setReferenceId);
        response.setExternalId);
        response.setAmount);
        response.setCurrency);
        response.setStatus.name());
        response.setDescription);

        // PIX fields
        response.setPixKey);
        response.setPixKeyType);
        response.setPixQrCode);
        response.setPixQrCodeText);
        response.setPixTransactionId);

        // Customer fields
        response.setCustomerEmail);
        response.setCustomerName);
        response.setCustomerDocument);

        // Financial fields
        response.setGrossAmount);
        response.setFeeAmount);
        response.setNetAmount);

        // Timestamps
        response.setCreatedAt);
        response.setExpiresAt);
        response.setProcessedAt);

        return response;
    }

    // Fallback methods for Circuit Breaker
    public PaymentResponse fallbackCreatePixPayment {
        log.error);
        throw new BusinessException;
    }

    // Inner class for PIX callback event
    public static class PixCallbackEvent {
        private final String pixTransactionId;
        private final boolean paid;
        private final String reason;
        private final String errorCode;

        public PixCallbackEvent {
            this.pixTransactionId = pixTransactionId;
            this.paid = paid;
            this.reason = reason;
            this.errorCode = errorCode;
        }

        public String getPixTransactionId { return pixTransactionId; }
        public boolean isPaid { return paid; }
        public String getReason { return reason; }
        public String getErrorCode { return errorCode; }
    }
}