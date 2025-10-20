package com.zendapag.core.pix.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
public class PixWebhookPayload {

    // Webhook metadata
    private String webhookId;
    private String eventType;
    private Instant timestamp;
    private String version = "1.0";

    // Transaction identification
    private String endToEndId;
    private String txId;
    private String referenceId;

    // Payment information
    private PaymentInfo payment;

    // Payer information
    private PayerInfo payer;

    // Additional data
    private Map<String, Object> additionalData;

    // Signature verification
    private String signature;
    private String signatureAlgorithm;

    @Data
    public static class PaymentInfo {
        private BigDecimal amount;
        private String currency = "BRL";
        private PixPaymentStatus status;
        private String statusReason;
        private Instant processedAt;
        private Instant paidAt;
        private String pixKey;
        private PixKeyType pixKeyType;
        private String description;
    }

    @Data
    public static class PayerInfo {
        private String name;
        private String document;
        private DocumentType documentType;
        private BankInfo bank;
    }

    @Data
    public static class BankInfo {
        private String ispb;
        private String name;
        private String agencyCode;
        private String accountNumber;
        private AccountType accountType;
    }

    public enum PixPaymentStatus {
        COMPLETED, EXPIRED, CANCELLED, REJECTED, ERROR
    }

    public enum PixKeyType {
        CPF, CNPJ, EMAIL, PHONE, EVP
    }

    public enum DocumentType {
        CPF, CNPJ
    }

    public enum AccountType {
        CHECKING, SAVINGS, PAYMENT
    }

    public enum EventType {
        PIX_PAYMENT_COMPLETED("pix.payment.completed"),
        PIX_PAYMENT_EXPIRED("pix.payment.expired"),
        PIX_PAYMENT_CANCELLED("pix.payment.cancelled"),
        PIX_PAYMENT_REJECTED("pix.payment.rejected"),
        PIX_PAYMENT_ERROR("pix.payment.error");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static EventType fromValue(String value) {
            for (EventType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown event type: " + value);
        }
    }

    // Helper methods
    public EventType getEventTypeEnum() {
        return EventType.fromValue(eventType);
    }

    public boolean isPaymentCompleted() {
        return payment != null && payment.status == PixPaymentStatus.COMPLETED;
    }

    public boolean isPaymentFailed() {
        return payment != null && (
            payment.status == PixPaymentStatus.EXPIRED ||
            payment.status == PixPaymentStatus.CANCELLED ||
            payment.status == PixPaymentStatus.REJECTED ||
            payment.status == PixPaymentStatus.ERROR
        );
    }
}