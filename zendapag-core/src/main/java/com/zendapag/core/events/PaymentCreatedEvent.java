package com.zendapag.core.events;

import com.zendapag.core.enums.PaymentMethod;
import com.zendapag.core.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a new payment is created in the system.
 * Contains all necessary information about the payment for downstream processing.
 */
public class PaymentCreatedEvent extends DomainEvent {

    @NotNull
    private final String paymentId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final String referenceId;

    @NotNull
    @Positive
    private final BigDecimal amount;

    @NotNull
    private final String currency;

    @NotNull
    private final PaymentMethod paymentMethod;

    @NotNull
    private final PaymentStatus status;

    private final String description;
    private final String customerName;
    private final String customerEmail;
    private final String customerDocument;
    private final String pixKey;
    private final String pixKeyType;
    private final Instant expiresAt;
    private final String callbackUrl;
    private final Map<String, Object> customFields;

    public PaymentCreatedEvent(String paymentId,
                              String merchantId,
                              String referenceId,
                              BigDecimal amount,
                              String currency,
                              PaymentMethod paymentMethod,
                              PaymentStatus status,
                              String description,
                              String customerName,
                              String customerEmail,
                              String customerDocument,
                              String pixKey,
                              String pixKeyType,
                              Instant expiresAt,
                              String callbackUrl,
                              Map<String, Object> customFields,
                              String correlationId) {

        super("payment_created", paymentId, "Payment", correlationId, null, 1L);

        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.referenceId = referenceId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.description = description;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerDocument = customerDocument;
        this.pixKey = pixKey;
        this.pixKeyType = pixKeyType;
        this.expiresAt = expiresAt;
        this.callbackUrl = callbackUrl;
        this.customFields = customFields != null ? new HashMap<>(customFields) : new HashMap<>();

        // Add contextual metadata
        this.withMetadata("merchant_id", merchantId)
            .withMetadata("payment_method", paymentMethod.toString())
            .withMetadata("amount", amount.toString())
            .withMetadata("currency", currency);
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", paymentId);
        data.put("merchantId", merchantId);
        data.put("referenceId", referenceId);
        data.put("amount", amount);
        data.put("currency", currency);
        data.put("paymentMethod", paymentMethod);
        data.put("status", status);
        data.put("description", description);
        data.put("customerName", customerName);
        data.put("customerEmail", customerEmail);
        data.put("customerDocument", customerDocument);
        data.put("pixKey", pixKey);
        data.put("pixKeyType", pixKeyType);
        data.put("expiresAt", expiresAt);
        data.put("callbackUrl", callbackUrl);
        data.put("customFields", customFields);
        return data;
    }

    // Getters
    public String getPaymentId() {
        return paymentId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public String getPixKey() {
        return pixKey;
    }

    public String getPixKeyType() {
        return pixKeyType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public Map<String, Object> getCustomFields() {
        return new HashMap<>(customFields);
    }

    // Utility methods
    public boolean hasExpiration() {
        return expiresAt != null;
    }

    public boolean isExpired() {
        return hasExpiration() && Instant.now().isAfter(expiresAt);
    }

    public boolean hasCallback() {
        return callbackUrl != null && !callbackUrl.isEmpty();
    }

    public boolean isPixPayment() {
        return PaymentMethod.PIX.equals(paymentMethod);
    }

    public boolean hasCustomerInfo() {
        return customerName != null || customerEmail != null || customerDocument != null;
    }

    public String getCustomField(String key) {
        Object value = customFields.get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public String getEventSummary() {
        return String.format("PaymentCreated[id=%s, merchant=%s, amount=%s %s, method=%s]",
                paymentId, merchantId, amount, currency, paymentMethod);
    }
}