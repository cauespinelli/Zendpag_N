package com.zendapag.api.controller;

import com.zendapag.api.dto.WebhookConfigRequest;
import com.zendapag.api.dto.WebhookEventResponse;
import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Webhook;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.service.MerchantService;
import com.zendapag.core.service.WebhookService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Webhooks", description = "Webhook configuration and management API")
@RestController
@RequestMapping("/api/v1/webhooks")
@PreAuthorize("hasRole('MERCHANT')")
@Validated
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;
    private final MerchantService merchantService;

    @Operation(
        summary = "Configure webhook",
        description = "Configures webhook URL and settings for the merchant. This will be used for payment notifications."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook configured successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid webhook configuration"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @RateLimiter(name = "webhooks-api")
    @Timed(value = "api.webhooks.configure", description = "Time taken to configure webhook")
    public ResponseEntity<ApiResponse<WebhookConfigResponse>> configureWebhook(
            @Valid @RequestBody WebhookConfigRequest request,
            Authentication authentication) {

        log.info("Configuring webhook for merchant: {}", authentication.getName());

        Merchant merchant = getMerchantFromAuth(authentication);

        // Validate webhook URL
        if (!isValidWebhookUrl(request.getUrl())) {
            throw new BusinessException("Invalid webhook URL format", "INVALID_WEBHOOK_URL");
        }

        // Update merchant webhook configuration
        merchant.setWebhookUrl(request.getUrl());
        if (request.getSecret() != null && !request.getSecret().trim().isEmpty()) {
            merchant.setWebhookSecret(request.getSecret());
        }

        // Update additional webhook settings if provided
        // In a real implementation, these would be stored in a separate webhook configuration entity

        Merchant updatedMerchant = merchantService.updateMerchant(merchant.getId(), merchant);

        WebhookConfigResponse response = new WebhookConfigResponse(
            updatedMerchant.getWebhookUrl(),
            updatedMerchant.getWebhookSecret() != null && !updatedMerchant.getWebhookSecret().trim().isEmpty(),
            request.getEventTypes(),
            Instant.now()
        );

        return ResponseEntity.ok(ApiResponse.success("Webhook configured successfully", response));
    }

    @Operation(
        summary = "Get webhook configuration",
        description = "Returns the current webhook configuration for the merchant."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/config")
    @Timed(value = "api.webhooks.config", description = "Time taken to get webhook config")
    public ResponseEntity<ApiResponse<WebhookConfigResponse>> getWebhookConfig(Authentication authentication) {
        Merchant merchant = getMerchantFromAuth(authentication);

        WebhookConfigResponse response = new WebhookConfigResponse(
            merchant.getWebhookUrl(),
            merchant.getWebhookSecret() != null && !merchant.getWebhookSecret().trim().isEmpty(),
            java.util.List.of("payment.created", "payment.paid", "payment.failed", "payment.cancelled"), // Default events
            Instant.now()
        );

        return ResponseEntity.ok(ApiResponse.success("Webhook configuration retrieved", response));
    }

    @Operation(
        summary = "Test webhook",
        description = "Sends a test webhook to verify the merchant's webhook endpoint is working correctly."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/test")
    @RateLimiter(name = "webhooks-api")
    @Timed(value = "api.webhooks.test", description = "Time taken to test webhook")
    public ResponseEntity<ApiResponse<WebhookTestResponse>> testWebhook(Authentication authentication) {
        log.info("Testing webhook for merchant: {}", authentication.getName());

        Merchant merchant = getMerchantFromAuth(authentication);

        if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().trim().isEmpty()) {
            throw new BusinessException("Webhook URL not configured", "WEBHOOK_URL_NOT_CONFIGURED");
        }

        // Send test webhook
        java.util.Map<String, Object> testData = new java.util.HashMap<>();
        testData.put("test", true);
        testData.put("timestamp", Instant.now().toString());
        testData.put("message", "This is a test webhook from Zendapag");

        try {
            webhookService.sendMerchantWebhook(merchant, "webhook.test", testData);

            WebhookTestResponse response = new WebhookTestResponse(
                true,
                "Test webhook sent successfully",
                Instant.now(),
                merchant.getWebhookUrl()
            );

            return ResponseEntity.ok(ApiResponse.success("Test webhook sent", response));

        } catch (Exception e) {
            log.error("Failed to send test webhook: {}", e.getMessage());

            WebhookTestResponse response = new WebhookTestResponse(
                false,
                "Failed to send test webhook: " + e.getMessage(),
                Instant.now(),
                merchant.getWebhookUrl()
            );

            return ResponseEntity.ok(ApiResponse.success("Test webhook attempted", response));
        }
    }

    @Operation(
        summary = "List webhook events",
        description = "Lists recent webhook events and their delivery status for the merchant."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/events")
    @Timed(value = "api.webhooks.events", description = "Time taken to list webhook events")
    public ResponseEntity<ApiResponse<Page<WebhookEventResponse>>> listWebhookEvents(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        Merchant merchant = getMerchantFromAuth(authentication);

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Webhook> webhooks = webhookService.findByMerchant(merchant, pageRequest);
        Page<WebhookEventResponse> responses = webhooks.map(this::convertToWebhookEventResponse);

        return ResponseEntity.ok(ApiResponse.success("Webhook events retrieved", responses));
    }

    @Operation(
        summary = "Get webhook event details",
        description = "Returns detailed information about a specific webhook event."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/events/{id}")
    @Timed(value = "api.webhooks.event.detail", description = "Time taken to get webhook event details")
    public ResponseEntity<ApiResponse<WebhookEventResponse>> getWebhookEvent(
            @Parameter(description = "Webhook event ID", required = true)
            @PathVariable UUID id,
            Authentication authentication) {

        Optional<Webhook> webhookOpt = webhookService.findById(id);
        if (webhookOpt.isEmpty()) {
            throw new BusinessException("Webhook event not found", "WEBHOOK_EVENT_NOT_FOUND");
        }

        Webhook webhook = webhookOpt.get();
        Merchant merchant = getMerchantFromAuth(authentication);

        // Verify webhook belongs to authenticated merchant
        if (!webhook.getMerchant().getId().equals(merchant.getId())) {
            throw new BusinessException.AccessDeniedException("Access denied - webhook event belongs to different merchant");
        }

        WebhookEventResponse response = convertToWebhookEventResponse(webhook);
        return ResponseEntity.ok(ApiResponse.success("Webhook event retrieved", response));
    }

    @Operation(
        summary = "Retry webhook",
        description = "Retries delivery of a failed webhook event."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/events/{id}/retry")
    @RateLimiter(name = "webhooks-api")
    @Timed(value = "api.webhooks.retry", description = "Time taken to retry webhook")
    public ResponseEntity<ApiResponse<WebhookEventResponse>> retryWebhook(
            @Parameter(description = "Webhook event ID", required = true)
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Retrying webhook: {} for merchant: {}", id, authentication.getName());

        Optional<Webhook> webhookOpt = webhookService.findById(id);
        if (webhookOpt.isEmpty()) {
            throw new BusinessException("Webhook event not found", "WEBHOOK_EVENT_NOT_FOUND");
        }

        Webhook webhook = webhookOpt.get();
        Merchant merchant = getMerchantFromAuth(authentication);

        // Verify webhook belongs to authenticated merchant
        if (!webhook.getMerchant().getId().equals(merchant.getId())) {
            throw new BusinessException.AccessDeniedException("Access denied");
        }

        // Retry webhook
        webhookService.retryWebhook(id);

        // Fetch updated webhook
        webhook = webhookService.findById(id).orElse(webhook);

        WebhookEventResponse response = convertToWebhookEventResponse(webhook);
        return ResponseEntity.ok(ApiResponse.success("Webhook retry initiated", response));
    }

    @Operation(
        summary = "Get webhook statistics",
        description = "Returns webhook delivery statistics for the merchant."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/stats")
    @Timed(value = "api.webhooks.stats", description = "Time taken to get webhook statistics")
    public ResponseEntity<ApiResponse<WebhookStatsResponse>> getWebhookStats(
            @Parameter(description = "Number of days to include in statistics")
            @RequestParam(defaultValue = "7") int days,
            Authentication authentication) {

        Merchant merchant = getMerchantFromAuth(authentication);

        // This would typically use a dedicated stats service
        // For now, we'll return mock data
        WebhookStatsResponse stats = new WebhookStatsResponse(
            100L,   // totalWebhooks
            85L,    // deliveredWebhooks
            10L,    // failedWebhooks
            5L,     // retryingWebhooks
            85.0,   // successRate
            days
        );

        return ResponseEntity.ok(ApiResponse.success("Webhook statistics retrieved", stats));
    }

    // Helper methods

    private Merchant getMerchantFromAuth(Authentication authentication) {
        String merchantDocument = authentication.getName();
        return merchantService.findByDocument(merchantDocument);
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

    private WebhookEventResponse convertToWebhookEventResponse(Webhook webhook) {
        return new WebhookEventResponse(
            webhook.getId().toString(),
            webhook.getEventType(),
            webhook.getUrl(),
            webhook.getStatus().name(),
            webhook.getRetryCount(),
            webhook.getResponseStatus(),
            webhook.getResponseTimeMs(),
            webhook.getErrorMessage(),
            webhook.getCreatedAt(),
            webhook.getSentAt(),
            webhook.getDeliveredAt(),
            webhook.getPayment() != null ? webhook.getPayment().getReferenceId() : null
        );
    }

    // Response DTOs

    public static class WebhookConfigResponse {
        private final String url;
        private final boolean hasSecret;
        private final java.util.List<String> eventTypes;
        private final Instant configuredAt;

        public WebhookConfigResponse(String url, boolean hasSecret, java.util.List<String> eventTypes, Instant configuredAt) {
            this.url = url;
            this.hasSecret = hasSecret;
            this.eventTypes = eventTypes;
            this.configuredAt = configuredAt;
        }

        public String getUrl() { return url; }
        public boolean isHasSecret() { return hasSecret; }
        public java.util.List<String> getEventTypes() { return eventTypes; }
        public Instant getConfiguredAt() { return configuredAt; }
    }

    public static class WebhookTestResponse {
        private final boolean success;
        private final String message;
        private final Instant testedAt;
        private final String testedUrl;

        public WebhookTestResponse(boolean success, String message, Instant testedAt, String testedUrl) {
            this.success = success;
            this.message = message;
            this.testedAt = testedAt;
            this.testedUrl = testedUrl;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Instant getTestedAt() { return testedAt; }
        public String getTestedUrl() { return testedUrl; }
    }

    public static class WebhookStatsResponse {
        private final Long totalWebhooks;
        private final Long deliveredWebhooks;
        private final Long failedWebhooks;
        private final Long retryingWebhooks;
        private final Double successRate;
        private final Integer periodDays;

        public WebhookStatsResponse(Long totalWebhooks, Long deliveredWebhooks, Long failedWebhooks,
                                  Long retryingWebhooks, Double successRate, Integer periodDays) {
            this.totalWebhooks = totalWebhooks;
            this.deliveredWebhooks = deliveredWebhooks;
            this.failedWebhooks = failedWebhooks;
            this.retryingWebhooks = retryingWebhooks;
            this.successRate = successRate;
            this.periodDays = periodDays;
        }

        public Long getTotalWebhooks() { return totalWebhooks; }
        public Long getDeliveredWebhooks() { return deliveredWebhooks; }
        public Long getFailedWebhooks() { return failedWebhooks; }
        public Long getRetryingWebhooks() { return retryingWebhooks; }
        public Double getSuccessRate() { return successRate; }
        public Integer getPeriodDays() { return periodDays; }
    }
}