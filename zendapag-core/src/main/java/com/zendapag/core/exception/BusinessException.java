package com.zendapag.core.exception;

public class BusinessException extends ZendapagException {
    public BusinessException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessException(String message, String errorCode) {
        super(message, errorCode);
    }

    public BusinessException(String message, String errorCode, Object details) {
        super(message, errorCode, details);
    }

    // Specific business exceptions
    public static class InsufficientLimitsException extends BusinessException {
        public InsufficientLimitsException(String message) {
            super(message, "INSUFFICIENT_LIMITS");
        }
    }

    public static class InvalidMerchantException extends BusinessException {
        public InvalidMerchantException(String message) {
            super(message, "INVALID_MERCHANT");
        }
    }

    public static class DuplicatePaymentException extends BusinessException {
        public DuplicatePaymentException(String message) {
            super(message, "DUPLICATE_PAYMENT");
        }
    }

    public static class PaymentExpiredException extends BusinessException {
        public PaymentExpiredException(String message) {
            super(message, "PAYMENT_EXPIRED");
        }
    }

    public static class InvalidPaymentStatusException extends BusinessException {
        public InvalidPaymentStatusException(String message) {
            super(message, "INVALID_PAYMENT_STATUS");
        }
    }

    public static class RiskRejectionException extends BusinessException {
        public RiskRejectionException(String message) {
            super(message, "RISK_REJECTION");
        }
    }
}