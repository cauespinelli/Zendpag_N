package com.zendapag.core.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse {
    private String id;
    private String referenceId;
    private String externalId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;

    // Merchant (estabelecimento)
    private String merchantId;
    private String merchantName;

    // PIX specific fields
    private String pixKey;
    private String pixKeyType;
    private String pixQrCode;
    private String pixQrCodeText;
    private String pixTransactionId;

    // Customer information
    private String customerEmail;
    private String customerName;
    private String customerDocument;

    // Financial information
    private BigDecimal grossAmount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;

    // Card display info (PCI-compliant: SOMENTE dado não-sensível).
    // NUNCA PAN completo nem CVV — proibido por PCI-DSS (req. 3.2/3.3) e pelas
    // regras do projeto. Guardamos/expomos apenas máscara, bandeira e validade.
    private String paymentMethodType;   // PIX, CREDIT_CARD, ...
    private String cardBrand;           // VISA, MASTERCARD, ...
    private String cardLast4;           // últimos 4 dígitos
    private String cardMaskedNumber;    // "•••• •••• •••• 4242"
    private String cardExpiry;          // "MM/AA"
    private Integer installments;       // nº de parcelas
    private String threeDsStatus;       // status 3DS
    private String boletoDueDate;       // vencimento do boleto (yyyy-MM-dd)
    private String source;              // origem (multi-tenant): DIRETO ou origem externa

    // Timestamps
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant expiresAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant processedAt;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPixKey() {
        return pixKey;
    }

    public void setPixKey(String pixKey) {
        this.pixKey = pixKey;
    }

    public String getPixKeyType() {
        return pixKeyType;
    }

    public void setPixKeyType(String pixKeyType) {
        this.pixKeyType = pixKeyType;
    }

    public String getPixQrCode() {
        return pixQrCode;
    }

    public void setPixQrCode(String pixQrCode) {
        this.pixQrCode = pixQrCode;
    }

    public String getPixQrCodeText() {
        return pixQrCodeText;
    }

    public void setPixQrCodeText(String pixQrCodeText) {
        this.pixQrCodeText = pixQrCodeText;
    }

    public String getPixTransactionId() {
        return pixTransactionId;
    }

    public void setPixTransactionId(String pixTransactionId) {
        this.pixTransactionId = pixTransactionId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public void setCustomerDocument(String customerDocument) {
        this.customerDocument = customerDocument;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getPaymentMethodType() { return paymentMethodType; }
    public void setPaymentMethodType(String paymentMethodType) { this.paymentMethodType = paymentMethodType; }

    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }

    public String getCardMaskedNumber() { return cardMaskedNumber; }
    public void setCardMaskedNumber(String cardMaskedNumber) { this.cardMaskedNumber = cardMaskedNumber; }

    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }

    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }

    public String getThreeDsStatus() { return threeDsStatus; }
    public void setThreeDsStatus(String threeDsStatus) { this.threeDsStatus = threeDsStatus; }

    public String getBoletoDueDate() { return boletoDueDate; }
    public void setBoletoDueDate(String boletoDueDate) { this.boletoDueDate = boletoDueDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}