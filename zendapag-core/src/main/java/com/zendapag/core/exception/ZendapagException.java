package com.zendapag.core.exception;

public class ZendapagException extends RuntimeException {
    private final String errorCode;
    private final Object details;

    public ZendapagException(String message) {
        super(message);
        this.errorCode = "GENERIC_ERROR";
        this.details = null;
    }

    public ZendapagException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public ZendapagException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ZendapagException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERIC_ERROR";
        this.details = null;
    }

    public ZendapagException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getDetails() {
        return details;
    }
}