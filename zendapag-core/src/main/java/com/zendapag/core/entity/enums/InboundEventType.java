package com.zendapag.core.entity.enums;

/**
 * Tipo NORMALIZADO de evento de entrada — cada adapter de PSP traduz o formato
 * proprietário para um destes. O núcleo só conhece estes tipos.
 */
public enum InboundEventType {
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    WITHDRAWAL_COMPLETED,
    UNKNOWN
}
