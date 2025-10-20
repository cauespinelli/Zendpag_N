package com.zendapag.core.event.payment;

import com.zendapag.core.entity.Payment;
import com.zendapag.core.event.BaseEvent;

public class PaymentFailedEvent extends BaseEvent {
    private final Payment payment;
    private final String reason;
    private final String errorCode;

    public PaymentFailedEvent(Payment payment, String reason, String errorCode, String correlationId) {
        super("payment.failed", payment.getMerchant(), correlationId);
        this.payment = payment;
        this.reason = reason;
        this.errorCode = errorCode;
    }

    public Payment getPayment() {
        return payment;
    }

    public String getReason() {
        return reason;
    }

    public String getErrorCode() {
        return errorCode;
    }
}