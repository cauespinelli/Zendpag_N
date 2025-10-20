package com.zendapag.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Payment cancellation request")
public class PaymentCancelRequest {

    @Schema(description = "Reason for cancellation", example = "Customer requested cancellation")
    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;
}