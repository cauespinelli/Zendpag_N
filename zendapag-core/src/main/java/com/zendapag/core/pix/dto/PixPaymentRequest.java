package com.zendapag.core.pix.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PixPaymentRequest {

    // Transaction identification
    private String endToEndId;
    private String txId;
    private String referenceId;

    // Amount information
    private BigDecimal amount;
    private String currency = "BRL";

    // Payer information
    private PayerInfo payer;

    // Payee information (Zendapag)
    private PayeeInfo payee;

    // PIX key information
    private String pixKey;
    private PixKeyType pixKeyType;

    // Payment information
    private String description;
    private Instant expiresAt;
    private PaymentType paymentType = PaymentType.IMMEDIATE;

    // QR Code information
    private QrCodeInfo qrCode;

    // Additional information
    private String additionalInfo;

    @Data
    public static class PayerInfo {
        private String name;
        private String document;
        private DocumentType documentType;
        private String email;
        private String phone;
    }

    @Data
    public static class PayeeInfo {
        private String name;
        private String document;
        private DocumentType documentType;
        private String pixKey;
        private PixKeyType pixKeyType;
        private String bankIspb;
        private String agencyCode;
        private String accountNumber;
        private AccountType accountType;
    }

    @Data
    public static class QrCodeInfo {
        private boolean dynamic = true;
        private int expirationMinutes = 30;
        private boolean reusable = false;
        private String merchantCity;
        private String merchantCategoryCode;
    }

    public enum PixKeyType {
        CPF, CNPJ, EMAIL, PHONE, EVP
    }

    public enum DocumentType {
        CPF, CNPJ
    }

    public enum PaymentType {
        IMMEDIATE, SCHEDULED
    }

    public enum AccountType {
        CHECKING, SAVINGS, PAYMENT
    }
}