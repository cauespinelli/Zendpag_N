package com.zendapag.core.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreatePixPaymentRequest {

    @NotBlank(message = "Reference ID is required")
    @Size(max = 255, message = "Reference ID must not exceed 255 characters")
    private String referenceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Amount must not exceed 999,999.99")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "BRL|USD|EUR", message = "Currency must be BRL, USD, or EUR")
    private String currency = "BRL";

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Email(message = "Customer email must be valid")
    private String customerEmail;

    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    private String customerName;

    @Size(max = 20, message = "Customer document must not exceed 20 characters")
    private String customerDocument;

    @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Customer phone must be valid")
    private String customerPhone;

    @Size(max = 255, message = "PIX key must not exceed 255 characters")
    private String pixKey;

    @Pattern(regexp = "CPF|CNPJ|EMAIL|PHONE|EVP", message = "PIX key type must be valid")
    private String pixKeyType;

    private Integer expirationMinutes = 60;

    @Size(max = 1000, message = "Notification URL must not exceed 1000 characters")
    @Pattern(regexp = "https?://.+", message = "Notification URL must be a valid HTTP(S) URL")
    private String notificationUrl;

    @Size(max = 500, message = "External ID must not exceed 500 characters")
    private String externalId;

    // Getters and Setters
    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
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

    public Integer getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(Integer expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }

    public void setNotificationUrl(String notificationUrl) {
        this.notificationUrl = notificationUrl;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}