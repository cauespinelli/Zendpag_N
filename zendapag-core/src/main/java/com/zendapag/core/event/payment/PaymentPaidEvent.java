package com.zendapag.core.event.payment;

import com.zendapag.core.entity.Payment;
import com.zendapag.core.event.BaseEvent;

public class PaymentPaidEvent extends BaseEvent {
    private final Payment payment;
    private final String pixTransactionId;

    public PaymentPaidEvent(Payment payment, String pixTransactionId, String correlationId) {
        super("payment.paid", payment.getMerchant(), correlationId);
        this.payment = payment;
        this.pixTransactionId = pixTransactionId;
    }

    public Payment getPayment() {
        return payment;
    }

    public String getPixTransactionId() {
        return pixTransactionId;
    }
}