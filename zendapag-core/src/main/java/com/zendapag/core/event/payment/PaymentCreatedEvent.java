package com.zendapag.core.event.payment;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.event.BaseEvent;

public class PaymentCreatedEvent extends BaseEvent {
    private final Payment payment;

    public PaymentCreatedEvent(Payment payment, String correlationId) {
        super("payment.created", payment.getMerchant(), correlationId);
        this.payment = payment;
    }

    public Payment getPayment() {
        return payment;
    }
}