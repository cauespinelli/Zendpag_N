package com.zendapag.core.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
