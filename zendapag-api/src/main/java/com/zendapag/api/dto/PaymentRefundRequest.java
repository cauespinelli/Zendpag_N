package com.zendapag.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Payment refund request")
public class PaymentRefundRequest {

    @Schema(description = "Refund amount", example = "100.50")
    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be positive")
    @DecimalMax(value = "999999999.99", message = "Refund amount too large")
    @Digits(integer = 12, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @Schema(description = "Reason for refund", example = "Product not delivered")
    @NotBlank(message = "Refund reason is required")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;
}