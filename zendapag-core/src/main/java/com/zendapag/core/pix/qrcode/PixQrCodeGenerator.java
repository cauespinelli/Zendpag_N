package com.zendapag.core.pix.qrcode;

import com.zendapag.core.entity.Payment;
import com.zendapag.core.pix.config.PixConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PixQrCodeGenerator {

    private final PixConfig pixConfig;
    private final Map<String, String> qrCodeCache = new ConcurrentHashMap<>();

    @Autowired
    public PixQrCodeGenerator(PixConfig pixConfig) {
        this.pixConfig = pixConfig;
    }

    public PixQrCodeResult generateDynamicQrCode(Payment payment) {
        log.debug("Generating dynamic PIX QR code for payment: {}", payment.getReferenceId());

        try {
            String qrCodeText = buildDynamicPixPayload(payment);
            String qrCodeImage = generateQrCodeImage(qrCodeText);

            PixQrCodeResult result = new PixQrCodeResult(qrCodeText, qrCodeImage, true, payment.getExpiresAt());

            log.info("Dynamic PIX QR code generated for payment: {}", payment.getReferenceId());
            return result;

        } catch (Exception e) {
            log.error("Error generating dynamic PIX QR code for payment {}: {}", payment.getReferenceId(), e.getMessage(), e);
            throw new QrCodeGenerationException("Failed to generate dynamic PIX QR code", e);
        }
    }

    public PixQrCodeResult generateStaticQrCode(String pixKey, String pixKeyType, String merchantName, String merchantCity) {
        log.debug("Generating static PIX QR code for PIX key: {}", maskPixKey(pixKey));

        try {
            String cacheKey = pixKey + "_" + merchantName + "_" + merchantCity;
            String cachedQrCode = qrCodeCache.get(cacheKey);

            if (cachedQrCode != null) {
                log.debug("Returning cached static QR code for PIX key: {}", maskPixKey(pixKey));
                return new PixQrCodeResult(cachedQrCode, generateQrCodeImage(cachedQrCode), false, null);
            }

            String qrCodeText = buildStaticPixPayload(pixKey, pixKeyType, merchantName, merchantCity);
            String qrCodeImage = generateQrCodeImage(qrCodeText);

            // Cache static QR codes
            qrCodeCache.put(cacheKey, qrCodeText);

            PixQrCodeResult result = new PixQrCodeResult(qrCodeText, qrCodeImage, false, null);

            log.info("Static PIX QR code generated for PIX key: {}", maskPixKey(pixKey));
            return result;

        } catch (Exception e) {
            log.error("Error generating static PIX QR code for PIX key {}: {}", maskPixKey(pixKey), e.getMessage(), e);
            throw new QrCodeGenerationException("Failed to generate static PIX QR code", e);
        }
    }

    private String buildDynamicPixPayload(Payment payment) {
        StringBuilder payload = new StringBuilder();

        // Payload Format Indicator (ID 00)
        payload.append(formatTLV("00", "01"));

        // Point of Initiation Method (ID 01) - Dynamic QR Code
        payload.append(formatTLV("01", "12"));

        // Merchant Account Information (ID 26) - PIX
        String merchantInfo = buildMerchantAccountInfo(payment);
        payload.append(formatTLV("26", merchantInfo));

        // Merchant Category Code (ID 52)
        String categoryCode = pixConfig.getQrCode().getMerchantCategoryCode();
        payload.append(formatTLV("52", categoryCode));

        // Transaction Currency (ID 53) - 986 = BRL
        payload.append(formatTLV("53", "986"));

        // Transaction Amount (ID 54) - Only for fixed amount
        if (payment.getAmount() != null) {
            String amount = payment.getAmount().toPlainString();
            payload.append(formatTLV("54", amount));
        }

        // Country Code (ID 58)
        payload.append(formatTLV("58", "BR"));

        // Merchant Name (ID 59)
        String merchantName = payment.getMerchant().getName().length() > 25 ?
                             payment.getMerchant().getName().substring(0, 25) :
                             payment.getMerchant().getName();
        payload.append(formatTLV("59", merchantName));

        // Merchant City (ID 60)
        String merchantCity = pixConfig.getQrCode().getMerchantCity();
        if (payment.getMerchant().getAddress() != null) {
            Map<String, Object> address = payment.getMerchant().getAddress();
            if (address.containsKey("city")) {
                merchantCity = address.get("city").toString();
            }
        }
        payload.append(formatTLV("60", merchantCity.length() > 15 ? merchantCity.substring(0, 15) : merchantCity));

        // Additional Data Field Template (ID 62)
        String additionalData = buildAdditionalDataField(payment);
        if (!additionalData.isEmpty()) {
            payload.append(formatTLV("62", additionalData));
        }

        // Calculate CRC16 and append (ID 63)
        String crc = calculateCRC16(payload.toString() + "6304");
        payload.append("6304").append(crc);

        return payload.toString();
    }

    private String buildStaticPixPayload(String pixKey, String pixKeyType, String merchantName, String merchantCity) {
        StringBuilder payload = new StringBuilder();

        // Payload Format Indicator (ID 00)
        payload.append(formatTLV("00", "01"));

        // Point of Initiation Method (ID 01) - Static QR Code
        payload.append(formatTLV("01", "11"));

        // Merchant Account Information (ID 26) - PIX
        String merchantInfo = buildStaticMerchantAccountInfo(pixKey);
        payload.append(formatTLV("26", merchantInfo));

        // Merchant Category Code (ID 52)
        String categoryCode = pixConfig.getQrCode().getMerchantCategoryCode();
        payload.append(formatTLV("52", categoryCode));

        // Transaction Currency (ID 53) - 986 = BRL
        payload.append(formatTLV("53", "986"));

        // Country Code (ID 58)
        payload.append(formatTLV("58", "BR"));

        // Merchant Name (ID 59)
        String formattedMerchantName = merchantName.length() > 25 ? merchantName.substring(0, 25) : merchantName;
        payload.append(formatTLV("59", formattedMerchantName));

        // Merchant City (ID 60)
        String formattedMerchantCity = merchantCity.length() > 15 ? merchantCity.substring(0, 15) : merchantCity;
        payload.append(formatTLV("60", formattedMerchantCity));

        // Calculate CRC16 and append (ID 63)
        String crc = calculateCRC16(payload.toString() + "6304");
        payload.append("6304").append(crc);

        return payload.toString();
    }

    private String buildMerchantAccountInfo(Payment payment) {
        StringBuilder info = new StringBuilder();

        // GUI (ID 00) - Globally Unique Identifier
        info.append(formatTLV("00", "BR.GOV.BCB.PIX"));

        // PIX Key (ID 01)
        String pixKey = payment.getPixKey();
        if (pixKey != null && !pixKey.trim().isEmpty()) {
            info.append(formatTLV("01", pixKey));
        }

        // Description/URL (ID 02) - Transaction ID for dynamic QR codes
        info.append(formatTLV("02", payment.getReferenceId()));

        return info.toString();
    }

    private String buildStaticMerchantAccountInfo(String pixKey) {
        StringBuilder info = new StringBuilder();

        // GUI (ID 00) - Globally Unique Identifier
        info.append(formatTLV("00", "BR.GOV.BCB.PIX"));

        // PIX Key (ID 01)
        info.append(formatTLV("01", pixKey));

        return info.toString();
    }

    private String buildAdditionalDataField(Payment payment) {
        StringBuilder additionalData = new StringBuilder();

        // Bill Number (ID 01) - Payment Reference
        if (payment.getReferenceId() != null) {
            String referenceId = payment.getReferenceId().length() > 25 ?
                               payment.getReferenceId().substring(0, 25) :
                               payment.getReferenceId();
            additionalData.append(formatTLV("01", referenceId));
        }

        // Mobile Number (ID 02) - Merchant Phone
        if (payment.getMerchant().getPhoneNumber() != null) {
            String phone = payment.getMerchant().getPhoneNumber().length() > 25 ?
                          payment.getMerchant().getPhoneNumber().substring(0, 25) :
                          payment.getMerchant().getPhoneNumber();
            additionalData.append(formatTLV("02", phone));
        }

        // Store Label (ID 03) - Merchant Trading Name
        if (payment.getMerchant().getTradingName() != null) {
            String tradingName = payment.getMerchant().getTradingName().length() > 25 ?
                               payment.getMerchant().getTradingName().substring(0, 25) :
                               payment.getMerchant().getTradingName();
            additionalData.append(formatTLV("03", tradingName));
        }

        // Loyalty Number (ID 04) - Customer Document
        if (payment.getCustomerDocument() != null) {
            additionalData.append(formatTLV("04", payment.getCustomerDocument()));
        }

        // Customer Label (ID 05) - Description
        if (payment.getDescription() != null) {
            String description = payment.getDescription().length() > 25 ?
                               payment.getDescription().substring(0, 25) :
                               payment.getDescription();
            additionalData.append(formatTLV("05", description));
        }

        return additionalData.toString();
    }

    private String formatTLV(String tag, String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        int length = value.getBytes().length;
        return tag + String.format("%02d", length) + value;
    }

    private String calculateCRC16(String data) {
        int crc = 0xFFFF;
        byte[] bytes = data.getBytes();

        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }

        return String.format("%04X", crc);
    }

    private String generateQrCodeImage(String qrCodeText) throws IOException {
        PixConfig.ImageConfig imageConfig = pixConfig.getQrCode().getImage();

        // Simple QR code generation using basic graphics
        // In a real implementation, you would use a proper QR code library like ZXing
        BufferedImage image = generateSimpleQrCodeImage(qrCodeText, imageConfig);

        // Convert to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, imageConfig.getFormat().toLowerCase(), baos);
        byte[] imageBytes = baos.toByteArray();

        return "data:image/" + imageConfig.getFormat().toLowerCase() + ";base64," +
               Base64.getEncoder().encodeToString(imageBytes);
    }

    private BufferedImage generateSimpleQrCodeImage(String qrCodeText, PixConfig.ImageConfig imageConfig) {
        int size = imageConfig.getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Set background color
        Color backgroundColor = Color.decode(imageConfig.getBackgroundColor());
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, size, size);

        // In a real implementation, this would generate the actual QR code matrix
        // For now, we'll create a placeholder pattern
        Color foregroundColor = Color.decode(imageConfig.getForegroundColor());
        g2d.setColor(foregroundColor);

        // Create a simple pattern (placeholder)
        int margin = imageConfig.getMargin();
        int patternSize = size - (2 * margin);
        int cellSize = patternSize / 21; // Standard QR code is 21x21 modules minimum

        // Draw a simple grid pattern as placeholder
        for (int i = 0; i < 21; i++) {
            for (int j = 0; j < 21; j++) {
                if ((i + j) % 3 == 0) { // Simple pattern logic
                    int x = margin + (i * cellSize);
                    int y = margin + (j * cellSize);
                    g2d.fillRect(x, y, cellSize, cellSize);
                }
            }
        }

        // Draw finder patterns (corners)
        drawFinderPattern(g2d, margin, margin, cellSize);
        drawFinderPattern(g2d, margin + (14 * cellSize), margin, cellSize);
        drawFinderPattern(g2d, margin, margin + (14 * cellSize), cellSize);

        g2d.dispose();
        return image;
    }

    private void drawFinderPattern(Graphics2D g2d, int x, int y, int cellSize) {
        // Draw 7x7 finder pattern
        g2d.fillRect(x, y, 7 * cellSize, 7 * cellSize);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x + cellSize, y + cellSize, 5 * cellSize, 5 * cellSize);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(x + (2 * cellSize), y + (2 * cellSize), 3 * cellSize, 3 * cellSize);
    }

    public boolean validateQrCodeExpiration(Payment payment) {
        if (payment.getExpiresAt() == null) {
            return true; // No expiration set
        }

        return Instant.now().isBefore(payment.getExpiresAt());
    }

    public String formatExpirationTime(Instant expiresAt) {
        if (expiresAt == null) {
            return "Sem expiração";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                                                       .withZone(ZoneId.systemDefault());
        return formatter.format(expiresAt);
    }

    private String maskPixKey(String pixKey) {
        if (pixKey == null || pixKey.length() <= 4) {
            return pixKey;
        }
        return pixKey.substring(0, 2) + "***" + pixKey.substring(pixKey.length() - 2);
    }

    public void clearCache() {
        qrCodeCache.clear();
        log.info("PIX QR code cache cleared");
    }

    // Result class
    public static class PixQrCodeResult {
        private final String qrCodeText;
        private final String qrCodeImage;
        private final boolean dynamic;
        private final Instant expiresAt;

        public PixQrCodeResult(String qrCodeText, String qrCodeImage, boolean dynamic, Instant expiresAt) {
            this.qrCodeText = qrCodeText;
            this.qrCodeImage = qrCodeImage;
            this.dynamic = dynamic;
            this.expiresAt = expiresAt;
        }

        public String getQrCodeText() { return qrCodeText; }
        public String getQrCodeImage() { return qrCodeImage; }
        public boolean isDynamic() { return dynamic; }
        public Instant getExpiresAt() { return expiresAt; }

        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    // Custom exception class
    public static class QrCodeGenerationException extends RuntimeException {
        public QrCodeGenerationException(String message) {
            super(message);
        }

        public QrCodeGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}