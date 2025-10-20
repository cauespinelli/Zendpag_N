package com.zendapag.core.pix.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PixPaymentResponse {

    // Transaction identification
    private String endToEndId;
    private String txId;
    private String referenceId;

    // Status information
    private PixPaymentStatus status;
    private String statusReason;

    // Amount information
    private BigDecimal amount;
    private String currency;

    // Timing information
    private Instant createdAt;
    private Instant processedAt;
    private Instant expiresAt;
    private Instant paidAt;

    // PIX specific information
    private String pixKey;
    private PixKeyType pixKeyType;
    private String qrCodeText;
    private String qrCodeImage;

    // Payer information (when payment is completed)
    private PayerInfo payer;

    // Banking information
    private BankInfo bankInfo;

    // Error information
    private ErrorInfo error;

    // Additional information
    private String additionalInfo;

    @Data
    public static class PayerInfo {
        private String name;
        private String document;
        private String bankIspb;
        private String bankName;
    }

    @Data
    public static class BankInfo {
        private String ispb;
        private String name;
        private String agencyCode;
        private String accountNumber;
    }

    @Data
    public static class ErrorInfo {
        private String code;
        private String message;
        private String detail;
        private Instant timestamp;
    }

    public enum PixPaymentStatus {
        CREATED,        // Payment created but not yet available
        ACTIVE,         // Payment active and awaiting completion
        COMPLETED,      // Payment completed successfully
        EXPIRED,        // Payment expired
        CANCELLED,      // Payment cancelled
        REJECTED,       // Payment rejected by participant
        ERROR          // Payment processing error
    }

    public enum PixKeyType {
        CPF, CNPJ, EMAIL, PHONE, EVP
    }

    // Helper methods
    public boolean isCompleted() {
        return status == PixPaymentStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == PixPaymentStatus.ACTIVE;
    }

    public boolean isFinal() {
        return status == PixPaymentStatus.COMPLETED ||
               status == PixPaymentStatus.EXPIRED ||
               status == PixPaymentStatus.CANCELLED ||
               status == PixPaymentStatus.REJECTED ||
               status == PixPaymentStatus.ERROR;
    }
}