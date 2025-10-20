package com.zendapag.worker.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * WebhookSigner for HMAC-SHA256 signature generation and validation
 * Provides secure webhook authentication and integrity verification
 */
@Component
public class WebhookSigner {

    private static final Logger logger = LoggerFactory.getLogger(WebhookSigner.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300; // 5 minutes

    @Value("${zendapag.webhook.signing.secret:}")
    private String defaultSigningSecret;

    @Value("${zendapag.webhook.signing.algorithm:HmacSHA256}")
    private String signingAlgorithm;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate HMAC-SHA256 signature for webhook payload
     */
    public String generateSignature(String payload, String secret) {
        return generateSignature(payload, secret, Instant.now().getEpochSecond());
    }

    /**
     * Generate HMAC-SHA256 signature with timestamp
     */
    public String generateSignature(String payload, String secret, long timestamp) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        Objects.requireNonNull(secret, "Secret cannot be null");

        try {
            String signaturePayload = timestamp + "." + payload;

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] signature = mac.doFinal(signaturePayload.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = Base64.getEncoder().encodeToString(signature);

            logger.debug("Generated signature for payload length: {}", payload.length());

            return SIGNATURE_PREFIX + encodedSignature;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error generating webhook signature", e);
            throw new WebhookSigningException("Failed to generate signature", e);
        }
    }

    /**
     * Validate webhook signature with timestamp verification
     */
    public boolean validateSignature(String payload, String signature, String secret, long timestamp) {
        if (!StringUtils.hasText(payload) || !StringUtils.hasText(signature) || !StringUtils.hasText(secret)) {
            logger.warn("Invalid signature validation parameters");
            return false;
        }

        // Validate timestamp to prevent replay attacks
        if (!isTimestampValid(timestamp)) {
            logger.warn("Webhook timestamp is outside tolerance window: {}", timestamp);
            return false;
        }

        try {
            String expectedSignature = generateSignature(payload, secret, timestamp);
            boolean isValid = secureCompare(signature, expectedSignature);

            if (!isValid) {
                logger.warn("Webhook signature validation failed. Expected: {}, Received: {}",
                    expectedSignature.substring(0, 12) + "...",
                    signature.substring(0, Math.min(12, signature.length())) + "...");
            } else {
                logger.debug("Webhook signature validated successfully");
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * Validate signature without timestamp (for backward compatibility)
     */
    public boolean validateSignature(String payload, String signature, String secret) {
        if (!StringUtils.hasText(payload) || !StringUtils.hasText(signature) || !StringUtils.hasText(secret)) {
            logger.warn("Invalid signature validation parameters");
            return false;
        }

        try {
            // Try with current timestamp first
            long currentTimestamp = Instant.now().getEpochSecond();
            if (validateSignature(payload, signature, secret, currentTimestamp)) {
                return true;
            }

            // Try with older timestamps within tolerance window
            for (int i = 1; i <= TIMESTAMP_TOLERANCE_SECONDS; i += 30) {
                if (validateSignature(payload, signature, secret, currentTimestamp - i)) {
                    return true;
                }
            }

            // Fallback: validate without timestamp for legacy webhooks
            String expectedSignature = generateSignatureWithoutTimestamp(payload, secret);
            return secureCompare(signature, expectedSignature);

        } catch (Exception e) {
            logger.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * Generate signature without timestamp for legacy support
     */
    private String generateSignatureWithoutTimestamp(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = Base64.getEncoder().encodeToString(signature);

            return SIGNATURE_PREFIX + encodedSignature;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error generating legacy webhook signature", e);
            throw new WebhookSigningException("Failed to generate legacy signature", e);
        }
    }

    /**
     * Create webhook headers for outgoing webhooks
     */
    public WebhookHeaders createWebhookHeaders(String payload, String secret) {
        long timestamp = Instant.now().getEpochSecond();
        String signature = generateSignature(payload, secret, timestamp);
        String requestId = generateRequestId();

        return WebhookHeaders.builder()
            .signature(signature)
            .timestamp(String.valueOf(timestamp))
            .requestId(requestId)
            .contentType("application/json")
            .userAgent("Zendapag-Webhooks/1.0")
            .build();
    }

    /**
     * Generate secure request ID for webhook tracking
     */
    public String generateRequestId() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Generate webhook secret for new merchant
     */
    public String generateWebhookSecret() {
        byte[] secretBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(secretBytes);
        return Base64.getEncoder().encodeToString(secretBytes);
    }

    /**
     * Validate timestamp to prevent replay attacks
     */
    private boolean isTimestampValid(long timestamp) {
        long currentTimestamp = Instant.now().getEpochSecond();
        long timeDifference = Math.abs(currentTimestamp - timestamp);
        return timeDifference <= TIMESTAMP_TOLERANCE_SECONDS;
    }

    /**
     * Secure string comparison to prevent timing attacks
     */
    private boolean secureCompare(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }

    /**
     * Hash payload for logging and debugging (without exposing sensitive data)
     */
    public String hashPayload(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error hashing payload", e);
            return "hash-error";
        }
    }

    /**
     * Webhook headers data class
     */
    public static class WebhookHeaders {
        private final String signature;
        private final String timestamp;
        private final String requestId;
        private final String contentType;
        private final String userAgent;

        private WebhookHeaders(Builder builder) {
            this.signature = builder.signature;
            this.timestamp = builder.timestamp;
            this.requestId = builder.requestId;
            this.contentType = builder.contentType;
            this.userAgent = builder.userAgent;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getSignature() { return signature; }
        public String getTimestamp() { return timestamp; }
        public String getRequestId() { return requestId; }
        public String getContentType() { return contentType; }
        public String getUserAgent() { return userAgent; }

        public static class Builder {
            private String signature;
            private String timestamp;
            private String requestId;
            private String contentType;
            private String userAgent;

            public Builder signature(String signature) {
                this.signature = signature;
                return this;
            }

            public Builder timestamp(String timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder requestId(String requestId) {
                this.requestId = requestId;
                return this;
            }

            public Builder contentType(String contentType) {
                this.contentType = contentType;
                return this;
            }

            public Builder userAgent(String userAgent) {
                this.userAgent = userAgent;
                return this;
            }

            public WebhookHeaders build() {
                return new WebhookHeaders(this);
            }
        }
    }

    /**
     * Custom exception for webhook signing errors
     */
    public static class WebhookSigningException extends RuntimeException {
        public WebhookSigningException(String message) {
            super(message);
        }

        public WebhookSigningException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}