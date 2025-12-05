package com.zendapag.core.enums;

/**
 * Enum representing payment status for events.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    APPROVED,
    REJECTED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CHARGEBACK,
    EXPIRED
}
