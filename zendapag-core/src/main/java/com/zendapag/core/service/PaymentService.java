package com.zendapag.core.service;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.dto.request.CreatePixPaymentRequest;
import com.zendapag.core.dto.response.PaymentResponse;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.entity.enums.MerchantStatus;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final TransactionRepository transactionRepository;
    private final MerchantService merchantService;
    private final RiskService riskService;
    private final PixService pixService;
    private final TransactionService transactionService;
    private final WebhookService webhookService;
    private final AuditService auditService;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                         MerchantRepository merchantRepository,
                         TransactionRepository transactionRepository,
                         MerchantService merchantService,
                         RiskService riskService,
                         PixService pixService,
                         TransactionService transactionService,
                         WebhookService webhookService,
                         AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.merchantService = merchantService;
        this.riskService = riskService;
        this.pixService = pixService;
        this.transactionService = transactionService;
        this.webhookService = webhookService;
        this.auditService = auditService;
    }

    @Timed
    @CircuitBreaker(name = "payment-service")
    @RateLimiter(name = "payment-service")
    @Retry(name = "payment-service")
    public PaymentResponse createPixPayment(CreatePixPaymentRequest request) {
        log.info("Creating PIX payment for reference: {}", request.getReferenceId());

        try {
            // 1. Validate merchant
            Optional<Merchant> merchantOpt = merchantRepository.findById(UUID.randomUUID().toString());
            if (!merchantOpt.isPresent()) {
                throw new BusinessException("Merchant not found");
            }
            Merchant merchant = merchantOpt.get();

            // 2. Validate request and check duplicates
            validatePaymentRequest(request, merchant);

            // 3. Create payment entity
            Payment payment = createPaymentFromRequest(request, merchant);

            // 4. Risk analysis
            double fraudScore = 0.0; // Simplified
            payment.setFeeAmount(BigDecimal.ZERO);

            // 5. Calculate fees
            calculateFees(payment, merchant);

            // 6. Save payment
            Payment savedPayment = paymentRepository.save(payment);

            // 7. Generate PIX code - simplified version
            String pixCode = "MOCK_QR_CODE_" + UUID.randomUUID().toString();
            savedPayment.setPixQrCode(pixCode);
            savedPayment.setPixTransactionId("TX_" + System.currentTimeMillis());
            savedPayment = paymentRepository.save(savedPayment);

            // 8. Create financial transaction
            Transaction transaction = new Transaction();
            transaction.setPayment(savedPayment);
            transaction.setAmount(savedPayment.getAmount());
            transactionRepository.save(transaction);

            // 9. Audit log
            auditService.logAction(
                "PAYMENT",
                savedPayment.getId().toString(),
                AuditAction.CREATE,
                "PIX payment created"
            );

            // 10. Send webhook - simplified
            log.info("Webhook would be sent for payment: {}", savedPayment.getId());

            log.info("PIX payment created successfully: {}", savedPayment.getId());
            return convertToResponse(savedPayment);

        } catch (BusinessException e) {
            log.warn("Business validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating payment", e);
            throw new BusinessException("Failed to create payment: " + e.getMessage());
        }
    }

    @Cacheable(value = "payments", key = "#paymentId")
    @Transactional(readOnly = true)
    @Timed
    public Optional<Payment> findById(UUID paymentId) {
        return paymentRepository.findById(paymentId);
    }

    @Cacheable(value = "payments", key = "'ref-' + #referenceId")
    @Transactional(readOnly = true)
    public Optional<Payment> findByReferenceId(String referenceId) {
        return paymentRepository.findByReferenceId(referenceId);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findByMerchant(UUID merchantId, Pageable pageable) {
        Optional<Merchant> merchant = merchantService.findById(merchantId);
        if (!merchant.isPresent()) {
            throw new BusinessException("Invalid merchant: " + merchantId);
        }
        return paymentRepository.findByMerchant(merchant.get(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findAll(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    @Timed
    @CacheEvict(value = "payments", key = "#paymentId")
    public Payment cancelPayment(UUID paymentId, String reason) {
        log.info("Cancelling payment: {}", paymentId);

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (!paymentOpt.isPresent()) {
            throw new BusinessException("Payment not found: " + paymentId);
        }

        Payment payment = paymentOpt.get();

        if (!payment.isPending()) {
            throw new BusinessException(
                "Payment cannot be cancelled in current status: " + payment.getStatus());
        }

        try {
            PaymentStatus oldStatus = payment.getStatus();
            payment.cancel();
            payment.setFailedAt(Instant.now());

            Payment savedPayment = paymentRepository.save(payment);

            // Audit log
            Map<String, Object> oldValues = new HashMap<>();
            oldValues.put("status", oldStatus.name());

            Map<String, Object> newValues = new HashMap<>();
            newValues.put("status", PaymentStatus.CANCELLED.name());

            auditService.logAction(
                "PAYMENT",
                paymentId.toString(),
                AuditAction.CANCEL,
                reason,
                oldValues,
                newValues
            );

            log.info("Payment cancelled: {}", paymentId);
            return savedPayment;

        } catch (Exception e) {
            log.error("Error cancelling payment: {}", paymentId, e);
            throw new BusinessException("Failed to cancel payment: " + e.getMessage());
        }
    }

    @Timed
    @CacheEvict(value = "payments", key = "#paymentId")
    public Payment refundPayment(UUID paymentId, BigDecimal amount, String reason) {
        log.info("Refunding payment: {}, amount: {}", paymentId, amount);

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (!paymentOpt.isPresent()) {
            throw new BusinessException("Payment not found: " + paymentId);
        }

        Payment payment = paymentOpt.get();

        if (!payment.isApproved()) {
            throw new BusinessException("Can only refund approved payments");
        }

        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new BusinessException("Refund amount exceeds payment amount");
        }

        try {
            BigDecimal oldRefundedAmount = payment.getRefundedAmount();
            payment.refund(amount);

            Payment savedPayment = paymentRepository.save(payment);

            // Audit log
            Map<String, Object> oldValues = new HashMap<>();
            oldValues.put("refundedAmount", oldRefundedAmount);

            Map<String, Object> newValues = new HashMap<>();
            newValues.put("refundedAmount", savedPayment.getRefundedAmount());

            auditService.logAction(
                "PAYMENT",
                paymentId.toString(),
                AuditAction.REFUND,
                reason,
                oldValues,
                newValues
            );

            log.info("Payment refunded: {}", paymentId);
            return savedPayment;

        } catch (Exception e) {
            log.error("Error refunding payment: {}", paymentId, e);
            throw new BusinessException("Failed to refund payment: " + e.getMessage());
        }
    }

    // Private helper methods

    private void validatePaymentRequest(CreatePixPaymentRequest request, Merchant merchant) {
        // Check daily limits
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new BusinessException("Merchant is not active");
        }

        // Check for duplicate reference ID
        Optional<Payment> existingPayment = paymentRepository.findByReferenceId(request.getReferenceId());
        if (existingPayment.isPresent()) {
            throw new BusinessException("Duplicate payment reference: " + request.getReferenceId());
        }
    }

    private Payment createPaymentFromRequest(CreatePixPaymentRequest request, Merchant merchant) {
        Payment payment = new Payment();
        payment.setMerchant(merchant);
        payment.setReferenceId(request.getReferenceId());
        payment.setExternalId(request.getExternalId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setDescription(request.getDescription());
        payment.setStatus(PaymentStatus.PENDING);

        // Customer information
        payment.setCustomerEmail(request.getCustomerEmail());
        payment.setCustomerName(request.getCustomerName());
        payment.setCustomerDocument(request.getCustomerDocument());
        payment.setCustomerPhone(request.getCustomerPhone());

        // PIX information
        payment.setPixKey(request.getPixKey());

        // Set expiration
        if (request.getExpirationMinutes() != null) {
            payment.setExpiresAt(Instant.now().plus(request.getExpirationMinutes(), ChronoUnit.MINUTES));
        }

        // Notification URL
        payment.setNotificationUrl(request.getNotificationUrl());

        return payment;
    }

    private void calculateFees(Payment payment, Merchant merchant) {
        BigDecimal feeRate = merchant.getFeeRate() != null ? merchant.getFeeRate() : BigDecimal.valueOf(0.01);
        BigDecimal feeAmount = payment.getAmount().multiply(feeRate);

        payment.setFeeAmount(feeAmount);
        payment.setNetAmount(payment.getAmount().subtract(feeAmount));
    }

    private PaymentResponse convertToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId().toString());
        response.setReferenceId(payment.getReferenceId());
        response.setExternalId(payment.getExternalId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus().name());
        response.setDescription(payment.getDescription());

        // PIX fields
        response.setPixKey(payment.getPixKey());
        response.setPixQrCode(payment.getPixQrCode());
        response.setPixQrCodeText(payment.getPixQrCode());
        response.setPixTransactionId(payment.getPixTransactionId());

        // Customer fields
        response.setCustomerEmail(payment.getCustomerEmail());
        response.setCustomerName(payment.getCustomerName());
        response.setCustomerDocument(payment.getCustomerDocument());

        // Financial fields
        response.setGrossAmount(payment.getAmount());
        response.setFeeAmount(payment.getFeeAmount());
        response.setNetAmount(payment.getNetAmount());

        // Timestamps
        response.setCreatedAt(payment.getCreatedAt());
        response.setExpiresAt(payment.getExpiresAt());
        response.setProcessedAt(payment.getProcessedAt());

        return response;
    }

    // Fallback methods for Circuit Breaker
    public PaymentResponse fallbackCreatePixPayment(CreatePixPaymentRequest request, Exception ex) {
        log.error("Fallback: Payment creation failed for reference {}: {}", request.getReferenceId(), ex.getMessage());
        throw new BusinessException("Payment service temporarily unavailable");
    }
}
