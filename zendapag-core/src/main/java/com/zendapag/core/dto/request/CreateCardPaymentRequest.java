package com.zendapag.core.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Criação de cobrança com CARTÃO de crédito.
 *
 * PCI: recebemos um TOKEN do cartão (tokenização pelo parceiro), nunca PAN/CVV.
 * brand/lastFour/holderName são apenas para exibição.
 */
public class CreateCardPaymentRequest {

    @Size(max = 255, message = "referenceId deve ter no máximo 255 caracteres")
    private String referenceId;

    @NotNull(message = "amount é obrigatório")
    @DecimalMin(value = "0.01", message = "amount deve ser maior que 0")
    @DecimalMax(value = "999999.99", message = "amount não pode exceder 999.999,99")
    private BigDecimal amount;

    @NotNull(message = "installments é obrigatório")
    @Min(value = 1, message = "installments mínimo é 1")
    @Max(value = 12, message = "installments máximo é 12")
    private Integer installments = 1;

    @NotBlank(message = "cardToken é obrigatório (cartão tokenizado; nunca envie PAN/CVV)")
    @Size(max = 255)
    private String cardToken;

    @Size(max = 50)
    private String brand;

    @Pattern(regexp = "\\d{4}", message = "lastFour deve ter 4 dígitos")
    private String lastFour;

    @Min(value = 1, message = "expiryMonth entre 1 e 12")
    @Max(value = 12, message = "expiryMonth entre 1 e 12")
    private Integer expiryMonth;

    @Min(value = 2000, message = "expiryYear inválido")
    @Max(value = 2099, message = "expiryYear inválido")
    private Integer expiryYear;

    @Size(max = 100)
    private String holderName;

    @Size(max = 255)
    private String customerName;

    @Email(message = "customerEmail deve ser válido")
    private String customerEmail;

    @Size(max = 20)
    private String customerDocument;

    @Size(max = 1000)
    private String description;

    @Pattern(regexp = "https?://.+", message = "notificationUrl deve ser uma URL HTTP(S) válida")
    @Size(max = 1000)
    private String notificationUrl;

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }

    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }

    public Integer getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }

    public Integer getExpiryYear() { return expiryYear; }
    public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }

    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerDocument() { return customerDocument; }
    public void setCustomerDocument(String customerDocument) { this.customerDocument = customerDocument; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNotificationUrl() { return notificationUrl; }
    public void setNotificationUrl(String notificationUrl) { this.notificationUrl = notificationUrl; }
}
