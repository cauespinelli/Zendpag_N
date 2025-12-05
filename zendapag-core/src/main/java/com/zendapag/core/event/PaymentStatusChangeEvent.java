package com.zendapag.core.event;

import com.zendapag.core.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusChangeEvent {
    private String transactionId;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private LocalDateTime timestamp;

    public PaymentStatusChangeEvent(Object source, UUID paymentId, PaymentStatus newStatus, String reason) {
        this.transactionId = paymentId != null ? paymentId.toString() : null;
        this.newStatus = newStatus != null ? newStatus.name() : null;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }
}
