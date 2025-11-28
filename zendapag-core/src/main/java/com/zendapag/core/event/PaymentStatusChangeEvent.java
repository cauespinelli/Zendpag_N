package com.zendapag.core.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusChangeEvent {
    private String transactionId;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private LocalDateTime timestamp;
}
