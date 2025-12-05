package com.zendapag.core.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookEvent {
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
    private String merchantId;

    public PaymentWebhookEvent(Object source, UUID paymentId, Object payload) {
        this.transactionId = paymentId != null ? paymentId.toString() : null;
        this.status = "WEBHOOK_RECEIVED";
        this.timestamp = LocalDateTime.now();
    }
}
