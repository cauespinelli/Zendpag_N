package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.dto.request.CreatePixPaymentRequest;
import com.zendapag.core.dto.response.PaymentResponse;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment createPayment(Payment payment) {
        log.info("Creating payment for merchant: {} amount: {}",
            payment.getMerchant().getId(), payment.getAmount());

        validatePayment(payment);

        if (payment.getReferenceId() == null) {
            payment.setReferenceId(generateReferenceId());
        }
        if (payment.getStatus() == null) {
            payment.setStatus(PaymentStatus.PENDING);
        }
        if (payment.getCurrency() == null) {
            payment.setCurrency("BRL");
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created with ID: {} referenceId: {}", saved.getId(), saved.getReferenceId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Payment findById(UUID id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
    }

    @Transactional(readOnly = true)
    public Payment findByReferenceId(String referenceId) {
        return paymentRepository.findByReferenceId(referenceId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "referenceId", referenceId));
    }

    @Transactional(readOnly = true)
    public Page<Payment> findByMerchant(Merchant merchant, Pageable pageable) {
        return paymentRepository.findByMerchant(merchant, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findByMerchantAndStatus(Merchant merchant, PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByMerchantAndStatus(merchant, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findByMerchantAndCreatedAtBetween(Merchant merchant, Instant start, Instant end, Pageable pageable) {
        return paymentRepository.findByMerchantAndCreatedAtBetween(merchant, start, end, pageable);
    }

    @Transactional(readOnly = true)
    public List<Payment> findPendingPayments() {
        return paymentRepository.findAllByStatus(PaymentStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Payment> findExpiredPayments(Instant before) {
        return paymentRepository.findByStatusAndExpiresAtBefore(PaymentStatus.PENDING, before);
    }

    @Transactional
    public Payment updateStatus(UUID id, PaymentStatus status) {
        Payment payment = findById(id);
        PaymentStatus oldStatus = payment.getStatus();

        validateStatusTransition(oldStatus, status);

        payment.setStatus(status);

        if (status == PaymentStatus.APPROVED) {
            payment.setPaidAt(Instant.now());
        } else if (status == PaymentStatus.CANCELLED || status == PaymentStatus.FAILED) {
            payment.setCancelledAt(Instant.now());
        } else if (status == PaymentStatus.REFUNDED) {
            payment.setRefundedAt(Instant.now());
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} status updated from {} to {}", id, oldStatus, status);
        return saved;
    }

    @Transactional
    public Payment confirmPayment(UUID id) {
        return updateStatus(id, PaymentStatus.APPROVED);
    }

    @Transactional
    public Payment cancelPayment(UUID id, String reason) {
        Payment payment = findById(id);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Only pending payments can be cancelled");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledAt(Instant.now());
        payment.setCancellationReason(reason);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refundPayment(UUID id, String reason) {
        Payment payment = findById(id);

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new BusinessException("INVALID_STATUS", "Only paid payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(Instant.now());
        payment.setRefundReason(reason);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment expirePayment(UUID id) {
        Payment payment = findById(id);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Only pending payments can expire");
        }

        payment.setStatus(PaymentStatus.EXPIRED);
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public long countByMerchantAndCreatedAtBetween(Merchant merchant, Instant start, Instant end) {
        return paymentRepository.countByMerchantAndCreatedAtBetween(merchant, start, end);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumAmountByMerchantAndCreatedAtBetween(Merchant merchant, Instant start, Instant end) {
        BigDecimal sum = paymentRepository.sumAmountByMerchantAndCreatedAtBetween(merchant, start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal sumFeesByMerchantAndCreatedAtBetween(Merchant merchant, Instant start, Instant end) {
        BigDecimal sum = paymentRepository.sumFeesByMerchantAndCreatedAtBetween(merchant, start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private void validatePayment(Payment payment) {
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Payment amount must be positive");
        }
        if (payment.getMerchant() == null) {
            throw new BusinessException("MERCHANT_REQUIRED", "Merchant is required");
        }
    }

    private void validateStatusTransition(PaymentStatus from, PaymentStatus to) {
        if (from == PaymentStatus.APPROVED && to != PaymentStatus.REFUNDED) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                "Paid payments can only transition to REFUNDED");
        }
        if (from == PaymentStatus.CANCELLED || from == PaymentStatus.EXPIRED || from == PaymentStatus.REFUNDED) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                "Cannot transition from terminal status: " + from);
        }
    }

    private String generateReferenceId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public PaymentResponse createPixPayment(UUID merchantId, CreatePixPaymentRequest request) {
        log.info("Creating PIX payment for merchant: {} reference: {}", merchantId, request.getReferenceId());
        
        // Create payment entity
        Payment payment = new Payment();
        payment.setReferenceId(request.getReferenceId());
        payment.setExternalId(request.getExternalId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setDescription(request.getDescription());
        payment.setPixKey(request.getPixKey());
        payment.setCustomerEmail(request.getCustomerEmail());
        payment.setCustomerName(request.getCustomerName());
        payment.setCustomerDocument(request.getCustomerDocument());
        payment.setNotificationUrl(request.getNotificationUrl());
        payment.setStatus(PaymentStatus.PENDING);
        
        // Set expiration
        int expirationMinutes = request.getExpirationMinutes() != null ? request.getExpirationMinutes() : 60;
        payment.setExpiresAt(java.time.Instant.now().plusSeconds(expirationMinutes * 60L));
        
        // Save payment
        Payment saved = paymentRepository.save(payment);
        
        // Convert to response
        return convertToResponse(saved);
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
        response.setPixKey(payment.getPixKey());
        response.setPixQrCode(payment.getPixQrCode());
        response.setPixTransactionId(payment.getPixTransactionId());
        response.setCustomerEmail(payment.getCustomerEmail());
        response.setCustomerName(payment.getCustomerName());
        response.setCustomerDocument(payment.getCustomerDocument());
        response.setCreatedAt(payment.getCreatedAt());
        response.setExpiresAt(payment.getExpiresAt());
        return response;
    }
}