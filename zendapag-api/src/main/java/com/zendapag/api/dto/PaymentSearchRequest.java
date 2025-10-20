package com.zendapag.api.dto;

import com.zendapag.core.entity.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Payment search and filter parameters")
public class PaymentSearchRequest {

    @Schema(description = "Page number (0-based)", example = "0")
    @Min(value = 0, message = "Page number must be non-negative")
    private int page = 0;

    @Schema(description = "Page size", example = "20")
    @Min(value = 1, message = "Page size must be positive")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private int size = 20;

    @Schema(description = "Sort field", example = "createdAt")
    private String sortBy = "createdAt";

    @Schema(description = "Sort direction", example = "desc", allowableValues = {"asc", "desc"})
    private String sortDir = "desc";

    @Schema(description = "Filter by payment status")
    private PaymentStatus status;

    @Schema(description = "Filter by start date (YYYY-MM-DD)")
    private LocalDate startDate;

    @Schema(description = "Filter by end date (YYYY-MM-DD)")
    private LocalDate endDate;

    @Schema(description = "Filter by minimum amount")
    @DecimalMin(value = "0.00", message = "Minimum amount must be non-negative")
    private BigDecimal minAmount;

    @Schema(description = "Filter by maximum amount")
    @DecimalMin(value = "0.00", message = "Maximum amount must be non-negative")
    private BigDecimal maxAmount;

    @Schema(description = "Filter by customer email")
    private String customerEmail;

    @Schema(description = "Filter by reference ID")
    private String referenceId;

    @Schema(description = "Filter by external ID")
    private String externalId;
}