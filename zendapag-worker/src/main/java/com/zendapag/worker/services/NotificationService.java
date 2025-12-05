package com.zendapag.worker.services;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    public void notifyPaymentReceived(UUID paymentId, String customerEmail) {
        log.info("Notifying customer {} about payment: {}", customerEmail, paymentId);
        // TODO: Implement actual notification (email, SMS, push)
    }

    public void notifyPaymentCompleted(UUID paymentId, String customerEmail) {
        log.info("Notifying customer {} about completed payment: {}", customerEmail, paymentId);
    }

    public void notifyMerchant(UUID merchantId, String subject, String message) {
        log.info("Notifying merchant: {} - {}", merchantId, subject);
    }
}
