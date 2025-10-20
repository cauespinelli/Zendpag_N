package com.zendapag.api.exception;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Business exception: {} at {}", ex.getMessage(), request.getRequestURI());

        HttpStatus status = mapBusinessExceptionToHttpStatus(ex);

        ApiResponse<Void> response = ApiResponse.error(
            ex.getMessage(),
            ex.getErrorCode(),
            request.getRequestURI(),
            createErrorDetails(ex)
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(BusinessException.PaymentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentNotFoundException(
            BusinessException.PaymentNotFoundException ex, HttpServletRequest request) {

        log.warn("Payment not found: {} at {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
            "Payment not found",
            "PAYMENT_NOT_FOUND",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BusinessException.InvalidMerchantException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidMerchantException(
            BusinessException.InvalidMerchantException ex, HttpServletRequest request) {

        log.warn("Invalid merchant: {} at {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
            "Invalid merchant",
            "INVALID_MERCHANT",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(BusinessException.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            BusinessException.AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied: {} at {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
            "Access denied",
            "ACCESS_DENIED",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Validation error at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value",
                (existing, replacement) -> existing
            ));

        Map<String, Object> errorDetails = Map.of(
            "validation_errors", fieldErrors,
            "error_count", fieldErrors.size()
        );

        ApiResponse<Void> response = ApiResponse.error(
            "Validation failed",
            "VALIDATION_ERROR",
            request.getRequestURI(),
            errorDetails
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.warn("Constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> violations = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage,
                (existing, replacement) -> existing
            ));

        Map<String, Object> errorDetails = Map.of(
            "constraint_violations", violations,
            "violation_count", violations.size()
        );

        ApiResponse<Void> response = ApiResponse.error(
            "Constraint violation",
            "CONSTRAINT_VIOLATION",
            request.getRequestURI(),
            errorDetails
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed JSON at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
            "Malformed JSON request",
            "MALFORMED_JSON",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing parameter at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = Map.of(
            "missing_parameter", ex.getParameterName(),
            "parameter_type", ex.getParameterType()
        );

        ApiResponse<Void> response = ApiResponse.error(
            String.format("Required parameter '%s' is missing", ex.getParameterName()),
            "MISSING_PARAMETER",
            request.getRequestURI(),
            errorDetails
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeaderException(
            MissingRequestHeaderException ex, HttpServletRequest request) {

        log.warn("Missing header at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = Map.of(
            "missing_header", ex.getHeaderName()
        );

        ApiResponse<Void> response = ApiResponse.error(
            String.format("Required header '%s' is missing", ex.getHeaderName()),
            "MISSING_HEADER",
            request.getRequestURI(),
            errorDetails
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("Type mismatch at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = Map.of(
            "parameter", ex.getName(),
            "provided_value", ex.getValue() != null ? ex.getValue().toString() : "null",
            "expected_type", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        ApiResponse<Void> response = ApiResponse.error(
            String.format("Invalid value for parameter '%s'", ex.getName()),
            "TYPE_MISMATCH",
            request.getRequestURI(),
            errorDetails
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Method not supported at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = Map.of(
            "method", ex.getMethod(),
            "supported_methods", ex.getSupportedHttpMethods() != null ?
                ex.getSupportedHttpMethods().toString() : "unknown"
        );

        ApiResponse<Void> response = ApiResponse.error(
            String.format("HTTP method '%s' not supported", ex.getMethod()),
            "METHOD_NOT_SUPPORTED",
            request.getRequestURI(),
            errorDetails
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.warn("No handler found at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
            "Endpoint not found",
            "ENDPOINT_NOT_FOUND",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
            "Authentication failed",
            "AUTHENTICATION_FAILED",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {

        log.warn("Bad credentials at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
            "Invalid credentials",
            "INVALID_CREDENTIALS",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSpringAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Spring access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
            "Access denied - insufficient permissions",
            "INSUFFICIENT_PERMISSIONS",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitException(
            RequestNotPermitted ex, HttpServletRequest request) {

        log.warn("Rate limit exceeded at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
            "Rate limit exceeded - please try again later",
            "RATE_LIMIT_EXCEEDED",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerException(
            CallNotPermittedException ex, HttpServletRequest request) {

        log.warn("Circuit breaker open at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
            "Service temporarily unavailable - please try again later",
            "SERVICE_UNAVAILABLE",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        log.warn("File size exceeded at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = Map.of(
            "max_size", ex.getMaxUploadSize()
        );

        ApiResponse<Void> response = ApiResponse.error(
            "File size exceeds maximum allowed size",
            "FILE_SIZE_EXCEEDED",
            request.getRequestURI(),
            errorDetails
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMessage());

        String message = "Data integrity violation";
        String errorCode = "DATA_INTEGRITY_VIOLATION";

        // Try to extract more specific error information
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("duplicate key")) {
                message = "Duplicate entry - record already exists";
                errorCode = "DUPLICATE_ENTRY";
            } else if (ex.getMessage().contains("foreign key")) {
                message = "Referenced record not found";
                errorCode = "FOREIGN_KEY_VIOLATION";
            } else if (ex.getMessage().contains("not-null")) {
                message = "Required field is missing";
                errorCode = "NOT_NULL_VIOLATION";
            }
        }

        ApiResponse<Void> response = ApiResponse.error(
            message,
            errorCode,
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        // In production, don't expose internal error details
        String message = "An unexpected error occurred";
        Map<String, Object> errorDetails = null;

        // Only include debug info in development
        if (isDevelopmentEnvironment()) {
            message = ex.getMessage();
            errorDetails = Map.of(
                "exception_type", ex.getClass().getSimpleName(),
                "stack_trace_summary", getStackTraceSummary(ex)
            );
        }

        ApiResponse<Void> response = ApiResponse.error(
            message,
            "INTERNAL_SERVER_ERROR",
            request.getRequestURI(),
            errorDetails
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Helper methods

    private HttpStatus mapBusinessExceptionToHttpStatus(BusinessException ex) {
        // Map specific business exception types to HTTP status codes
        if (ex instanceof BusinessException.PaymentNotFoundException ||
            ex instanceof BusinessException.MerchantNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }

        if (ex instanceof BusinessException.InvalidMerchantException ||
            ex instanceof BusinessException.AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        }

        if (ex instanceof BusinessException.InvalidPaymentStatusException ||
            ex instanceof BusinessException.DuplicatePaymentException) {
            return HttpStatus.CONFLICT;
        }

        if (ex instanceof BusinessException.InsufficientLimitsException) {
            return HttpStatus.PAYMENT_REQUIRED;
        }

        // Default to bad request for other business exceptions
        return HttpStatus.BAD_REQUEST;
    }

    private Map<String, Object> createErrorDetails(BusinessException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", Instant.now());

        if (ex.getErrorCode() != null) {
            details.put("error_code", ex.getErrorCode());
        }

        // Add any additional context from the exception
        return details;
    }

    private boolean isDevelopmentEnvironment() {
        String activeProfile = System.getProperty("spring.profiles.active", "");
        return activeProfile.contains("dev") || activeProfile.contains("local");
    }

    private String getStackTraceSummary(Exception ex) {
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace.length == 0) {
            return "No stack trace available";
        }

        // Return first few stack trace elements
        StringBuilder summary = new StringBuilder();
        int limit = Math.min(3, stackTrace.length);

        for (int i = 0; i < limit; i++) {
            StackTraceElement element = stackTrace[i];
            summary.append(element.toString());
            if (i < limit - 1) {
                summary.append(" -> ");
            }
        }

        return summary.toString();
    }
}