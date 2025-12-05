package com.zendapag.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Slf4j
public class WebhookSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${zendapag.webhook.secret:default-secret-key}")
    private String defaultSecret;

    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            log.error("Failed to sign webhook payload", e);
            throw new RuntimeException("Failed to sign webhook payload", e);
        }
    }

    public String generateSignature(String payload) {
        return sign(payload, defaultSecret);
    }

    public String generateSignature(String payload, String secret) {
        return sign(payload, secret != null ? secret : defaultSecret);
    }

    public boolean verify(String payload, String signature, String secret) {
        String expectedSignature = sign(payload, secret);
        return expectedSignature.equals(signature);
    }

    public boolean verify(String payload, String signature) {
        return verify(payload, signature, defaultSecret);
    }
}
