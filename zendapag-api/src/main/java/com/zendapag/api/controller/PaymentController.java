package com.zendapag.api.controller;

import com.zendapag.api.dto.PaymentCancelRequest;
import com.zendapag.api.dto.PaymentRefundRequest;
import com.zendapag.api.dto.PaymentSearchRequest;
import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.dto.request.CreatePixPaymentRequest;
import com.zendapag.core.dto.response.PaymentResponse;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.service.MerchantService;
import com.zendapag.core.service.PaymentService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Payments", description = "Payment processing API for PIX transactions")
@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasRole('MERCHANT')")
@Validated
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final MerchantService merchantService;

    @Operation(
        summary = "Create PIX payment",
        description = "Creates a new PIX payment with QR code generation. Returns payment details including QR code for customer scanning."
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "201", description = "Payment created successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request data"),
        @SwaggerApiResponse(responseCode = "401", description = "Authentication required"),
        @SwaggerApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @SwaggerApiResponse(responseCode = "429", description = "Rate limit exceeded"),
        @SwaggerApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/pix")
    @RateLimiter(name = "payments-api")
    @Timed(name = "api.payments.create.pix", description = "Time taken to create PIX payment via API")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPixPayment(
            @Valid @RequestBody CreatePixPaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        log.info("Creating PIX payment for merchant: {} reference: {}",
            authentication.getName(), request.getReferenceId());

        try {
            // Get merchant from authentication
            UUID merchantId = getMerchantIdFromAuth(authentication);

            // Add request metadata
            enrichRequestWithMetadata(request, httpRequest);

            // Create payment
            PaymentResponse response = paymentService.createPixPayment(merchantId, request);

            log.info("PIX payment created successfully: {} for merchant: {}",
                response.getReferenceId(), authentication.getName());

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PIX payment created successfully", response));

        } catch (BusinessException e) {
            log.warn("Business error creating PIX payment: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating PIX payment: {}", e.getMessage(), e);
            throw new BusinessException("Failed to process payment request", "PAYMENT_PROCESSING_ERROR");
        }
    }

    @Operation(
        summary = "Get payment by ID",
        description = "Retrieves payment details by payment ID. Only accessible by the payment's merchant."
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Payment found"),
        @SwaggerApiResponse(responseCode = "404", description = "Payment not found"),
        @SwaggerApiResponse(responseCode = "403", description = "Access denied - payment belongs to different merchant")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @Timed(name = "api.payments.get", description = "Time taken to get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable UUID id,
            Authentication authentication) {

        log.debug("Getting payment: {} for merchant: {}", id, authentication.getName());

        Optional<Payment> paymentOpt = paymentService.findById(id);
        if (paymentOpt.isEmpty()) {
            throw new BusinessException.PaymentNotFoundException("Payment not found");
        }

        Payment payment = paymentOpt.get();
        UUID merchantId = getMerchantIdFromAuth(authentication);

        // Verify payment belongs to authenticated merchant
        if (!payment.getMerchant().getId().equals(merchantId)) {
            throw new BusinessException.AccessDeniedException("Access denied - payment belongs to different merchant");
        }

        PaymentResponse response = convertToPaymentResponse(payment);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", response));
    }

    @Operation(
        summary = "Get payment by reference ID",
        description = "Retrieves payment details by merchant's reference ID."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/reference/{referenceId}")
    @Timed(name = "api.payments.get.reference", description = "Time taken to get payment by reference")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByReference(
            @Parameter(description = "Merchant reference ID", required = true)
            @PathVariable @NotBlank String referenceId,
            Authentication authentication) {

        Optional<Payment> paymentOpt = paymentService.findByReferenceId(referenceId);
        if (paymentOpt.isEmpty()) {
            throw new BusinessException.PaymentNotFoundException("Payment not found");
        }

        Payment payment = paymentOpt.get();
        UUID merchantId = getMerchantIdFromAuth(authentication);

        // Verify payment belongs to authenticated merchant
        if (!payment.getMerchant().getId().equals(merchantId)) {
            throw new BusinessException.AccessDeniedException("Access denied");
        }

        PaymentResponse response = convertToPaymentResponse(payment);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", response));
    }

    @Operation(
        summary = "List payments",
        description = "Lists payments for the authenticated merchant with pagination and filtering options."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @Timed(name = "api.payments.list", description = "Time taken to list payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> listPayments(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Filter by payment status")
            @RequestParam(required = false) PaymentStatus status,
            @Parameter(description = "Filter by start date (YYYY-MM-DD)")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date (YYYY-MM-DD)")
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Filter by minimum amount")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Filter by maximum amount")
            @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Filter by customer email")
            @RequestParam(required = false) String customerEmail,
            Authentication authentication) {

        UUID merchantId = getMerchantIdFromAuth(authentication);

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        // Build search specification
        Specification<Payment> spec = buildPaymentSearchSpec(merchantId, status, startDate, endDate,
            minAmount, maxAmount, customerEmail);

        Page<Payment> payments = paymentService.findAll(spec, pageRequest);
        Page<PaymentResponse> responses = payments.map(this::convertToPaymentResponse);

        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", responses));
    }

    @Operation(
        summary = "Cancel payment",
        description = "Cancels a pending payment. Only pending payments can be cancelled."
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Payment cancelled successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Payment cannot be cancelled in current status"),
        @SwaggerApiResponse(responseCode = "404", description = "Payment not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/cancel")
    @RateLimiter(name = "payments-api")
    @Timed(name = "api.payments.cancel", description = "Time taken to cancel payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody PaymentCancelRequest request,
            Authentication authentication) {

        log.info("Cancelling payment: {} for merchant: {} reason: {}",
            id, authentication.getName(), request.getReason());

        // Verify payment belongs to merchant
        verifyPaymentOwnership(id, authentication);

        Payment cancelledPayment = paymentService.cancelPayment(id, request.getReason());
        PaymentResponse response = convertToPaymentResponse(cancelledPayment);

        return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully", response));
    }

    @Operation(
        summary = "Refund payment",
        description = "Processes a full or partial refund for an approved payment."
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Refund processed successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid refund amount or payment cannot be refunded"),
        @SwaggerApiResponse(responseCode = "404", description = "Payment not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/refund")
    @RateLimiter(name = "payments-api")
    @Timed(name = "api.payments.refund", description = "Time taken to refund payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody PaymentRefundRequest request,
            Authentication authentication) {

        log.info("Processing refund for payment: {} amount: {} for merchant: {}",
            id, request.getAmount(), authentication.getName());

        // Verify payment belongs to merchant
        verifyPaymentOwnership(id, authentication);

        Payment refundedPayment = paymentService.refundPayment(id, request.getAmount(), request.getReason());
        PaymentResponse response = convertToPaymentResponse(refundedPayment);

        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }

    @Operation(
        summary = "Get payment statistics",
        description = "Returns aggregated payment statistics for the merchant."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/stats")
    @Timed(name = "api.payments.stats", description = "Time taken to get payment statistics")
    public ResponseEntity<ApiResponse<PaymentStatsResponse>> getPaymentStats(
            @Parameter(description = "Start date for stats (YYYY-MM-DD)")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "End date for stats (YYYY-MM-DD)")
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {

        UUID merchantId = getMerchantIdFromAuth(authentication);

        // Default to last 30 days if no dates provided
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        // This would typically use a dedicated stats service
        PaymentStatsResponse stats = calculatePaymentStats(merchantId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success("Payment statistics retrieved", stats));
    }

    @Operation(
        summary = "Get payment QR code",
        description = "Retrieves the PIX QR code for a payment (image data)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/qr-code")
    @Timed(name = "api.payments.qrcode", description = "Time taken to get payment QR code")
    public ResponseEntity<ApiResponse<QrCodeResponse>> getPaymentQrCode(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable UUID id,
            Authentication authentication) {

        Optional<Payment> paymentOpt = paymentService.findById(id);
        if (paymentOpt.isEmpty()) {
            throw new BusinessException.PaymentNotFoundException("Payment not found");
        }

        Payment payment = paymentOpt.get();
        UUID merchantId = getMerchantIdFromAuth(authentication);

        // Verify payment belongs to authenticated merchant
        if (!payment.getMerchant().getId().equals(merchantId)) {
            throw new BusinessException.AccessDeniedException("Access denied");
        }

        if (payment.getPixQrCode() == null) {
            throw new BusinessException("QR code not available for this payment", "QR_CODE_NOT_AVAILABLE");
        }

        QrCodeResponse response = new QrCodeResponse(payment.getPixQrCode(), payment.getPixQrCodeText());
        return ResponseEntity.ok(ApiResponse.success("QR code retrieved", response));
    }

    // Helper methods

    private UUID getMerchantIdFromAuth(Authentication authentication) {
        // This would typically extract the merchant ID from JWT token claims
        // For now, we'll assume the username is the merchant document and look it up
        String merchantDocument = authentication.getName();
        Optional<Merchant> merchantOpt = merchantService.findByDocument(merchantDocument);

        if (merchantOpt.isEmpty()) {
            throw new BusinessException.InvalidMerchantException("Merchant not found");
        }

        return merchantOpt.get().getId();
    }

    private void enrichRequestWithMetadata(CreatePixPaymentRequest request, HttpServletRequest httpRequest) {
        // Add IP address and user agent for fraud detection
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        request.setIpAddress(ipAddress);
        request.setUserAgent(userAgent);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private void verifyPaymentOwnership(UUID paymentId, Authentication authentication) {
        Optional<Payment> paymentOpt = paymentService.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            throw new BusinessException.PaymentNotFoundException("Payment not found");
        }

        Payment payment = paymentOpt.get();
        UUID merchantId = getMerchantIdFromAuth(authentication);

        if (!payment.getMerchant().getId().equals(merchantId)) {
            throw new BusinessException.AccessDeniedException("Access denied - payment belongs to different merchant");
        }
    }

    private PaymentResponse convertToPaymentResponse(Payment payment) {
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
        response.setFeeAmount(payment.getFeeAmount());
        response.setNetAmount(payment.getNetAmount());
        response.setCreatedAt(payment.getCreatedAt());
        response.setExpiresAt(payment.getExpiresAt());
        response.setProcessedAt(payment.getProcessedAt());
        response.setRefundedAmount(payment.getRefundedAmount());
        response.setRefundableAmount(payment.getRefundableAmount());
        return response;
    }

    private Specification<Payment> buildPaymentSearchSpec(UUID merchantId, PaymentStatus status,
            LocalDate startDate, LocalDate endDate, BigDecimal minAmount, BigDecimal maxAmount,
            String customerEmail) {

        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Merchant filter (always required)
            predicates.add(criteriaBuilder.equal(root.get("merchant").get("id"), merchantId));

            // Status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Date range filter
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), startDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toInstant()));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThan(
                    root.get("createdAt"), endDate.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toInstant()));
            }

            // Amount range filter
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            // Customer email filter
            if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("customerEmail")),
                    "%" + customerEmail.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private PaymentStatsResponse calculatePaymentStats(UUID merchantId, LocalDate startDate, LocalDate endDate) {
        // This is a simplified implementation
        // In a real scenario, you'd use a dedicated service with optimized queries
        return new PaymentStatsResponse(0L, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L);
    }

    // Response DTOs

    public static class QrCodeResponse {
        private final String qrCodeImage;
        private final String qrCodeText;

        public QrCodeResponse(String qrCodeImage, String qrCodeText) {
            this.qrCodeImage = qrCodeImage;
            this.qrCodeText = qrCodeText;
        }

        public String getQrCodeImage() { return qrCodeImage; }
        public String getQrCodeText() { return qrCodeText; }
    }

    public static class PaymentStatsResponse {
        private final Long totalPayments;
        private final BigDecimal totalAmount;
        private final BigDecimal averageAmount;
        private final Long approvedPayments;
        private final Long pendingPayments;
        private final Long failedPayments;

        public PaymentStatsResponse(Long totalPayments, BigDecimal totalAmount, BigDecimal averageAmount,
                                  Long approvedPayments, Long pendingPayments, Long failedPayments) {
            this.totalPayments = totalPayments;
            this.totalAmount = totalAmount;
            this.averageAmount = averageAmount;
            this.approvedPayments = approvedPayments;
            this.pendingPayments = pendingPayments;
            this.failedPayments = failedPayments;
        }

        public Long getTotalPayments() { return totalPayments; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getAverageAmount() { return averageAmount; }
        public Long getApprovedPayments() { return approvedPayments; }
        public Long getPendingPayments() { return pendingPayments; }
        public Long getFailedPayments() { return failedPayments; }
    }
}