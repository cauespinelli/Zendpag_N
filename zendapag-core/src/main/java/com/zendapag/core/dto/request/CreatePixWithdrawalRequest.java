package com.zendapag.core.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO para requisição de criação de saque PIX
 */
public class CreatePixWithdrawalRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "50000.00", message = "Amount must not exceed 50,000.00")
    private BigDecimal amount;

    @NotBlank(message = "PIX key is required")
    @Size(max = 255, message = "PIX key must not exceed 255 characters")
    private String pixKey;

    @NotBlank(message = "PIX key type is required")
    @Pattern(regexp = "CPF|CNPJ|EMAIL|PHONE|RANDOM", message = "PIX key type must be CPF, CNPJ, EMAIL, PHONE, or RANDOM")
    private String pixKeyType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 255, message = "External reference must not exceed 255 characters")
    private String externalReference;

    @Size(max = 255, message = "Recipient name must not exceed 255 characters")
    private String recipientName;

    @Size(max = 20, message = "Recipient document must not exceed 20 characters")
    private String recipientDocument;

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientDocument() {
        return recipientDocument;
    }

    public void setRecipientDocument(String recipientDocument) {
        this.recipientDocument = recipientDocument;
    }
}
