package com.zendapag.core.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Criação de cobrança com BOLETO. Boleto é assíncrono: a criação emite o boleto
 * (código de barras, linha digitável, vencimento) e o pagamento fica PENDING até
 * a confirmação via webhook de entrada.
 */
public class CreateBoletoPaymentRequest {

    @Size(max = 255, message = "referenceId deve ter no máximo 255 caracteres")
    private String referenceId;

    @NotNull(message = "amount é obrigatório")
    @DecimalMin(value = "0.01", message = "amount deve ser maior que 0")
    @DecimalMax(value = "999999.99", message = "amount não pode exceder 999.999,99")
    private BigDecimal amount;

    @Min(value = 1, message = "dueInDays mínimo é 1")
    @Max(value = 60, message = "dueInDays máximo é 60")
    private Integer dueInDays;

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

    public Integer getDueInDays() { return dueInDays; }
    public void setDueInDays(Integer dueInDays) { this.dueInDays = dueInDays; }

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
