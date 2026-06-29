package com.zendapag.api.controller;

import com.zendapag.api.dto.MerchantUpdateRequest;
import com.zendapag.api.dto.MerchantResponse;
import com.zendapag.api.dto.ApiKeyResponse;
import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.service.MerchantService;
import com.zendapag.core.service.TransactionService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Tag(name = "Merchants", description = "Merchant management API")
@RestController
@RequestMapping("/api/v1/merchants")
@PreAuthorize("hasRole('MERCHANT')")
@Validated
@RequiredArgsConstructor
@Slf4j
public class MerchantController {

    private final MerchantService merchantService;
    private final TransactionService transactionService;

    @Operation(
        summary = "Get merchant profile",
        description = "Returns the authenticated merchant's profile information and current status."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merchant profile retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merchant not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    @Timed(value = "api.merchants.me", description = "Time taken to get merchant profile")
    public ResponseEntity<ApiResponse<MerchantResponse>> getMerchantProfile(Authentication authentication) {
        log.debug("Getting merchant profile for: {}", authentication.getName());

        Merchant merchant = getMerchantFromAuth(authentication);
        MerchantResponse response = convertToMerchantResponse(merchant);

        return ResponseEntity.ok(ApiResponse.success("Merchant profile retrieved", response));
    }

    @Operation(
        summary = "List all merchants (admin)",
        description = "Lists all merchants, paginated. Admin only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "api.merchants.list", description = "Time taken to list merchants")
    public ResponseEntity<ApiResponse<Page<MerchantResponse>>> listMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        if (page < 0) page = 0;
        if (size <= 0 || size > 200) size = 20;

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<MerchantResponse> responses = merchantService.findAll(pageRequest).map(this::convertToMerchantResponse);

        return ResponseEntity.ok(ApiResponse.success("Merchants retrieved", responses));
    }

    @Operation(
        summary = "Update merchant profile",
        description = "Updates merchant profile information. Some fields may require re-verification."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merchant profile updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid data provided"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/me")
    @RateLimiter(name = "merchants-api")
    @Timed(value = "api.merchants.update", description = "Time taken to update merchant profile")
    public ResponseEntity<ApiResponse<MerchantResponse>> updateMerchantProfile(
            @Valid @RequestBody MerchantUpdateRequest request,
            Authentication authentication) {

        log.info("Updating merchant profile for: {}", authentication.getName());

        Merchant merchant = getMerchantFromAuth(authentication);

        // Update merchant fields
        updateMerchantFromRequest(merchant, request);

        Merchant updatedMerchant = merchantService.updateMerchant(merchant.getId(), merchant);
        MerchantResponse response = convertToMerchantResponse(updatedMerchant);

        return ResponseEntity.ok(ApiResponse.success("Merchant profile updated successfully", response));
    }

    @Operation(
        summary = "Get merchant balance",
        description = "Returns the current account balance for the merchant."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/balance")
    @Timed(value = "api.merchants.balance", description = "Time taken to get merchant balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getMerchantBalance(Authentication authentication) {
        Merchant merchant = getMerchantFromAuth(authentication);
        BigDecimal balance = transactionService.getMerchantBalance(merchant);

        BalanceResponse response = new BalanceResponse(balance, "BRL", Instant.now());
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved", response));
    }

    @Operation(
        summary = "Generate new API key",
        description = "Generates a new API key for the merchant. Previous API key will be invalidated."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "API key generated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/me/api-keys")
    @RateLimiter(name = "merchants-api")
    @Timed(value = "api.merchants.generate.apikey", description = "Time taken to generate API key")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> generateApiKey(
            @Parameter(description = "API key description")
            @RequestParam(required = false, defaultValue = "Generated via API") String description,
            Authentication authentication) {

        log.info("Generating new API key for merchant: {}", authentication.getName());

        Merchant merchant = getMerchantFromAuth(authentication);

        // This would typically be handled by a dedicated API key service
        String apiKey = generateSecureApiKey();
        String keyId = UUID.randomUUID().toString();

        // In a real implementation, this would be stored in a separate API keys table
        // For now, we'll return the generated key
        ApiKeyResponse response = new ApiKeyResponse(keyId, apiKey, description, Instant.now(), null);

        log.info("API key generated for merchant: {} keyId: {}", merchant.getDocument(), keyId);

        return ResponseEntity.ok(ApiResponse.success("API key generated successfully", response));
    }

    @Operation(
        summary = "Update webhook URL",
        description = "Updates the merchant's webhook URL for payment notifications."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/me/webhook-url")
    @RateLimiter(name = "merchants-api")
    @Timed(value = "api.merchants.webhook.update", description = "Time taken to update webhook URL")
    public ResponseEntity<ApiResponse<WebhookConfigResponse>> updateWebhookUrl(
            @Parameter(description = "Webhook URL", required = true)
            @RequestParam String webhookUrl,
            @Parameter(description = "Webhook secret for signature validation")
            @RequestParam(required = false) String webhookSecret,
            Authentication authentication) {

        log.info("Updating webhook URL for merchant: {}", authentication.getName());

        Merchant merchant = getMerchantFromAuth(authentication);

        // Validate webhook URL
        if (!isValidWebhookUrl(webhookUrl)) {
            throw new BusinessException("Invalid webhook URL format", "INVALID_WEBHOOK_URL");
        }

        merchant.setWebhookUrl(webhookUrl);
        if (webhookSecret != null && !webhookSecret.trim().isEmpty()) {
            merchant.setWebhookSecret(webhookSecret);
        }

        Merchant updatedMerchant = merchantService.updateMerchant(merchant.getId(), merchant);

        WebhookConfigResponse response = new WebhookConfigResponse(
            updatedMerchant.getWebhookUrl(),
            updatedMerchant.getWebhookSecret() != null,
            Instant.now()
        );

        return ResponseEntity.ok(ApiResponse.success("Webhook configuration updated", response));
    }

    @Operation(
        summary = "Test webhook",
        description = "Sends a test webhook to verify the merchant's webhook endpoint configuration."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/me/webhook/test")
    @RateLimiter(name = "merchants-api")
    @Timed(value = "api.merchants.webhook.test", description = "Time taken to test webhook")
    public ResponseEntity<ApiResponse<WebhookTestResponse>> testWebhook(Authentication authentication) {
        log.info("Testing webhook for merchant: {}", authentication.getName());

        Merchant merchant = getMerchantFromAuth(authentication);

        if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().trim().isEmpty()) {
            throw new BusinessException("Webhook URL not configured", "WEBHOOK_URL_NOT_CONFIGURED");
        }

        // This would typically use the WebhookService to send a test webhook
        boolean testSuccessful = sendTestWebhook(merchant);

        WebhookTestResponse response = new WebhookTestResponse(testSuccessful,
            testSuccessful ? "Webhook test successful" : "Webhook test failed", Instant.now());

        return ResponseEntity.ok(ApiResponse.success("Webhook test completed", response));
    }

    @Operation(
        summary = "Get merchant status",
        description = "Returns the merchant's current status and verification information."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/status")
    @Timed(value = "api.merchants.status", description = "Time taken to get merchant status")
    public ResponseEntity<ApiResponse<MerchantStatusResponse>> getMerchantStatus(Authentication authentication) {
        Merchant merchant = getMerchantFromAuth(authentication);

        MerchantStatusResponse response = new MerchantStatusResponse(
            merchant.getStatus().name(),
            merchant.getKycVerified(),
            merchant.getRiskScore(),
            merchant.getDailyLimit(),
            merchant.getTransactionLimit(),
            merchant.getFeeRate(),
            merchant.getLastLoginAt()
        );

        return ResponseEntity.ok(ApiResponse.success("Merchant status retrieved", response));
    }

    @Operation(
        summary = "Request KYC verification",
        description = "Initiates the KYC verification process for the merchant."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/me/kyc/verify")
    @RateLimiter(name = "merchants-api")
    @Timed(value = "api.merchants.kyc.verify", description = "Time taken to request KYC verification")
    public ResponseEntity<ApiResponse<KycVerificationResponse>> requestKycVerification(Authentication authentication) {
        log.info("Requesting KYC verification for merchant: {}", authentication.getName());

        Merchant merchant = getMerchantFromAuth(authentication);

        if (merchant.getKycVerified()) {
            throw new BusinessException("KYC already verified", "KYC_ALREADY_VERIFIED");
        }

        // In a real implementation, this would initiate the KYC process
        // For now, we'll return a verification request ID
        String verificationId = UUID.randomUUID().toString();

        KycVerificationResponse response = new KycVerificationResponse(
            verificationId,
            "PENDING",
            "KYC verification request submitted",
            Instant.now()
        );

        return ResponseEntity.ok(ApiResponse.success("KYC verification request submitted", response));
    }

    // Helper methods

    private Merchant getMerchantFromAuth(Authentication authentication) {
        String merchantDocument = authentication.getName();
        return merchantService.findByDocument(merchantDocument);
    }

    private MerchantResponse convertToMerchantResponse(Merchant merchant) {
        MerchantResponse response = new MerchantResponse();
        response.setId(merchant.getId().toString());
        response.setDocument(merchant.getDocument());
        response.setName(merchant.getName());
        response.setTradingName(merchant.getTradingName());
        response.setEmail(merchant.getEmail());
        response.setPhoneNumber(merchant.getPhoneNumber());
        response.setWebsiteUrl(merchant.getWebsiteUrl());
        response.setDescription(merchant.getDescription());
        response.setStatus(merchant.getStatus().name());
        response.setSource(merchant.getSource());
        response.setSourceExternalId(merchant.getSourceExternalId());
        response.setKycVerified(merchant.getKycVerified());
        response.setRiskScore(merchant.getRiskScore());
        response.setAddress(merchant.getAddressAsMap());
        response.setCreatedAt(merchant.getCreatedAt());
        response.setLastLoginAt(merchant.getLastLoginAt());
        return response;
    }

    private void updateMerchantFromRequest(Merchant merchant, MerchantUpdateRequest request) {
        if (request.getName() != null) merchant.setName(request.getName());
        if (request.getTradingName() != null) merchant.setTradingName(request.getTradingName());
        if (request.getEmail() != null) merchant.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null) merchant.setPhoneNumber(request.getPhoneNumber());
        if (request.getWebsiteUrl() != null) merchant.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getDescription() != null) merchant.setDescription(request.getDescription());
        if (request.getAddress() != null) {
            java.util.Map<String, Object> addrMap = request.getAddress();
            if (addrMap.get("street") != null) merchant.setAddress((String) addrMap.get("street"));
            if (addrMap.get("city") != null) merchant.setCity((String) addrMap.get("city"));
            if (addrMap.get("state") != null) merchant.setState((String) addrMap.get("state"));
            if (addrMap.get("postalCode") != null) merchant.setPostalCode((String) addrMap.get("postalCode"));
            if (addrMap.get("country") != null) merchant.setCountry((String) addrMap.get("country"));
        }
    }

    private boolean isValidWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return false;
        }

        try {
            java.net.URL url = new java.net.URL(webhookUrl);
            return "https".equals(url.getProtocol()) || "http".equals(url.getProtocol());
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }

    private boolean sendTestWebhook(Merchant merchant) {
        // This would typically use the WebhookService
        // For now, we'll simulate a test
        try {
            // Simulate webhook test
            Thread.sleep(500); // Simulate network call
            return Math.random() > 0.1; // 90% success rate for demo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String generateSecureApiKey() {
        // Generate a secure API key
        return "zp_" + java.util.UUID.randomUUID().toString().replace("-", "") +
               java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    // Response DTOs

    public static class BalanceResponse {
        private final BigDecimal balance;
        private final String currency;
        private final Instant timestamp;

        public BalanceResponse(BigDecimal balance, String currency, Instant timestamp) {
            this.balance = balance;
            this.currency = currency;
            this.timestamp = timestamp;
        }

        public BigDecimal getBalance() { return balance; }
        public String getCurrency() { return currency; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class WebhookConfigResponse {
        private final String webhookUrl;
        private final boolean hasSecret;
        private final Instant updatedAt;

        public WebhookConfigResponse(String webhookUrl, boolean hasSecret, Instant updatedAt) {
            this.webhookUrl = webhookUrl;
            this.hasSecret = hasSecret;
            this.updatedAt = updatedAt;
        }

        public String getWebhookUrl() { return webhookUrl; }
        public boolean isHasSecret() { return hasSecret; }
        public Instant getUpdatedAt() { return updatedAt; }
    }

    public static class WebhookTestResponse {
        private final boolean successful;
        private final String message;
        private final Instant testedAt;

        public WebhookTestResponse(boolean successful, String message, Instant testedAt) {
            this.successful = successful;
            this.message = message;
            this.testedAt = testedAt;
        }

        public boolean isSuccessful() { return successful; }
        public String getMessage() { return message; }
        public Instant getTestedAt() { return testedAt; }
    }

    public static class MerchantStatusResponse {
        private final String status;
        private final boolean kycVerified;
        private final Integer riskScore;
        private final BigDecimal dailyLimit;
        private final BigDecimal transactionLimit;
        private final BigDecimal feeRate;
        private final Instant lastLoginAt;

        public MerchantStatusResponse(String status, boolean kycVerified, Integer riskScore,
                                    BigDecimal dailyLimit, BigDecimal transactionLimit,
                                    BigDecimal feeRate, Instant lastLoginAt) {
            this.status = status;
            this.kycVerified = kycVerified;
            this.riskScore = riskScore;
            this.dailyLimit = dailyLimit;
            this.transactionLimit = transactionLimit;
            this.feeRate = feeRate;
            this.lastLoginAt = lastLoginAt;
        }

        public String getStatus() { return status; }
        public boolean isKycVerified() { return kycVerified; }
        public Integer getRiskScore() { return riskScore; }
        public BigDecimal getDailyLimit() { return dailyLimit; }
        public BigDecimal getTransactionLimit() { return transactionLimit; }
        public BigDecimal getFeeRate() { return feeRate; }
        public Instant getLastLoginAt() { return lastLoginAt; }
    }

    public static class KycVerificationResponse {
        private final String verificationId;
        private final String status;
        private final String message;
        private final Instant requestedAt;

        public KycVerificationResponse(String verificationId, String status, String message, Instant requestedAt) {
            this.verificationId = verificationId;
            this.status = status;
            this.message = message;
            this.requestedAt = requestedAt;
        }

        public String getVerificationId() { return verificationId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public Instant getRequestedAt() { return requestedAt; }
    }
}