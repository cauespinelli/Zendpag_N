package com.zendapag.core.service;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.pix.qrcode.PixQrCodeGenerator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PixService {

    private final PixQrCodeGenerator qrCodeGenerator;
    private final AuditService auditService;

    // Regex patterns for PIX key validation
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern CNPJ_PATTERN = Pattern.compile("^\\d{14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?55\\d{10,11}$");
    private static final Pattern EVP_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    @Autowired
    public PixService(
            PixQrCodeGenerator qrCodeGenerator,
            AuditService auditService) {
        this.qrCodeGenerator = qrCodeGenerator;
        this.auditService = auditService;
    }

    @Timed
    @CircuitBreaker(name = "pix-service")
    @Retry(name = "pix-service")
    public PixQrCodeGenerator.PixQrCodeResult generatePixQrCode(Payment payment) {
        log.info("Generating PIX QR Code for payment: {}", payment.getId());

        try {
            // Generate dynamic QR code using PIX components
            PixQrCodeGenerator.PixQrCodeResult result = qrCodeGenerator.generateDynamicQrCode(payment);

            // Audit log
            auditService.logAction(
                "PAYMENT",
                payment.getId().toString(),
                AuditAction.CREATE,
                "PIX QR code generated"
            );

            log.info("PIX QR Code generated successfully for payment: {}", payment.getId());
            return result;

        } catch (Exception e) {
            log.error("Failed to generate PIX QR Code for payment: {}", payment.getId(), e);
            auditService.logFailure(
                "PAYMENT",
                payment.getId().toString(),
                AuditAction.CREATE,
                "PIX QR code generation failed",
                e
            );
            throw new BusinessException("Failed to generate PIX QR Code: " + e.getMessage());
        }
    }

    @Timed
    public PixQrCodeGenerator.PixQrCodeResult generateStaticQrCode(String pixKey, String amount, String description) {
        log.debug("Generating static QR Code for PIX key: {}", maskPixKey(pixKey));

        try {
            return qrCodeGenerator.generateStaticQrCode(pixKey, amount, description);
        } catch (Exception e) {
            log.error("Failed to generate static QR Code", e);
            throw new BusinessException("Failed to generate static PIX QR Code: " + e.getMessage());
        }
    }

    @Timed
    @CircuitBreaker(name = "pix-service")
    @Retry(name = "pix-service")
    public boolean validatePixKey(String pixKey, String pixKeyType) {
        log.debug("Validating PIX key: {} of type: {}", maskPixKey(pixKey), pixKeyType);

        try {
            boolean isValid = switch (pixKeyType.toUpperCase()) {
                case "CPF" -> CPF_PATTERN.matcher(pixKey.replaceAll("[^0-9]", "")).matches();
                case "CNPJ" -> CNPJ_PATTERN.matcher(pixKey.replaceAll("[^0-9]", "")).matches();
                case "EMAIL" -> EMAIL_PATTERN.matcher(pixKey.toLowerCase()).matches();
                case "PHONE" -> PHONE_PATTERN.matcher(formatPhone(pixKey)).matches();
                case "EVP" -> EVP_PATTERN.matcher(pixKey.toLowerCase()).matches();
                default -> false;
            };

            log.debug("PIX key validation result: {} is {}", maskPixKey(pixKey), isValid ? "valid" : "invalid");
            return isValid;

        } catch (Exception e) {
            log.warn("PIX key validation failed for key {}: {}", maskPixKey(pixKey), e.getMessage());
            // Return false for validation failures to avoid blocking payments
            return false;
        }
    }

    public String detectPixKeyType(String pixKey) {
        String cleaned = pixKey.replaceAll("[^0-9a-zA-Z@.+-]", "");

        if (cleaned.contains("@")) {
            return "EMAIL";
        }

        if (cleaned.startsWith("+55") || cleaned.matches("^\\d{10,11}$")) {
            return "PHONE";
        }

        if (cleaned.matches("^\\d{11}$")) {
            return "CPF";
        }

        if (cleaned.matches("^\\d{14}$")) {
            return "CNPJ";
        }

        if (cleaned.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            return "EVP";
        }

        throw new BusinessException("Unable to determine PIX key type");
    }

    // QR Code utility methods
    public boolean validateQrCodeExpiration(Payment payment) {
        return qrCodeGenerator.validateQrCodeExpiration(payment);
    }

    public String formatExpirationTime(Payment payment) {
        return qrCodeGenerator.formatExpirationTime(payment);
    }

    public void clearQrCodeCache() {
        qrCodeGenerator.clearCache();
    }

    // Helper methods
    private String maskPixKey(String pixKey) {
        if (pixKey == null || pixKey.length() <= 4) {
            return "***";
        }
        return pixKey.substring(0, 2) + "***" + pixKey.substring(pixKey.length() - 2);
    }

    private String formatPhone(String phone) {
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (!cleaned.startsWith("55")) {
            cleaned = "55" + cleaned;
        }
        return "+" + cleaned;
    }

    // Fallback methods for Circuit Breaker
    public PixQrCodeGenerator.PixQrCodeResult fallbackGeneratePixQrCode(Payment payment, Exception ex) {
        log.warn("Fallback: Using placeholder QR Code for payment: {}", payment.getId());

        String placeholderText = "00020101021226580014BR.GOV.BCB.PIX0136" +
                                payment.getReferenceId() +
                                "520400005303986" +
                                "6304XXXX";
        String placeholderImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

        return new PixQrCodeGenerator.PixQrCodeResult(
            placeholderText,
            placeholderImage,
            "FALLBACK_" + UUID.randomUUID().toString()
        );
    }

    public boolean fallbackValidatePixKey(String pixKey, String pixKeyType, Exception ex) {
        log.warn("Fallback: PIX key validation failed, returning true to avoid blocking: {}", maskPixKey(pixKey));
        return true; // Return true to avoid blocking payments
    }
}
