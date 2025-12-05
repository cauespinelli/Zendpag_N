package com.zendapag.core.pix.qrcode;

import com.zendapag.core.entity.Payment;
import com.zendapag.core.pix.config.PixConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PixQrCodeGenerator {

    private final PixConfig pixConfig;
    private final Map<String, String> qrCodeCache = new ConcurrentHashMap<>();

    @Autowired
    public PixQrCodeGenerator(PixConfig pixConfig) { this.pixConfig = pixConfig; }

    public PixQrCodeResult generateDynamicQrCode(Payment payment) {
        log.debug("Generating dynamic PIX QR code for payment: {}", payment.getReferenceId());
        String qrCodeText = "00020101021226580014BR.GOV.BCB.PIX0136" + payment.getReferenceId() + "520400005303986" + "6304XXXX";
        String qrCodeImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        return new PixQrCodeResult(qrCodeText, qrCodeImage, true, payment.getExpiresAt());
    }

    public PixQrCodeResult generateStaticQrCode(String pixKey, String pixKeyType, String merchantName, String merchantCity) {
        log.debug("Generating static PIX QR code for key");
        String qrCodeText = "00020101021126580014BR.GOV.BCB.PIX01" + pixKey + "520400005303986" + "6304XXXX";
        String qrCodeImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        return new PixQrCodeResult(qrCodeText, qrCodeImage, false, null);
    }

    public boolean validateQrCodeExpiration(Payment payment) {
        if (payment.getExpiresAt() == null) return true;
        return Instant.now().isBefore(payment.getExpiresAt());
    }

    public String formatExpirationTime(Instant expiresAt) {
        if (expiresAt == null) return "Sem expiracao";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
        return formatter.format(expiresAt);
    }

    public void clearCache() { qrCodeCache.clear(); log.info("PIX QR code cache cleared"); }

    public static class PixQrCodeResult {
        private final String qrCodeText;
        private final String qrCodeImage;
        private final boolean dynamic;
        private final Instant expiresAt;

        public PixQrCodeResult(String qrCodeText, String qrCodeImage, boolean dynamic, Instant expiresAt) {
            this.qrCodeText = qrCodeText; this.qrCodeImage = qrCodeImage; this.dynamic = dynamic; this.expiresAt = expiresAt;
        }
        public String getQrCodeText() { return qrCodeText; }
        public String getQrCodeImage() { return qrCodeImage; }
        public boolean isDynamic() { return dynamic; }
        public Instant getExpiresAt() { return expiresAt; }
        public boolean isExpired() { return expiresAt != null && Instant.now().isAfter(expiresAt); }
    }

    public static class QrCodeGenerationException extends RuntimeException {
        public QrCodeGenerationException(String message) { super(message); }
        public QrCodeGenerationException(String message, Throwable cause) { super(message, cause); }
    }
}
