package com.zendapag.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Webhook configuration request")
public class WebhookConfigRequest {

    @Schema(description = "Webhook URL (must be HTTPS in production)", example = "https://api.merchant.com/webhooks/zendapag")
    @NotBlank(message = "Webhook URL is required")
    @Size(max = 500, message = "Webhook URL must be at most 500 characters")
    private String url;

    @Schema(description = "Secret for webhook signature validation", example = "your-secret-key")
    @Size(max = 255, message = "Webhook secret must be at most 255 characters")
    private String secret;

    @Schema(description = "Event types to receive webhooks for")
    private List<String> eventTypes = List.of(
        "payment.created",
        "payment.paid",
        "payment.failed",
        "payment.cancelled",
        "payment.refunded"
    );

    @Schema(description = "Enable webhook", example = "true")
    private Boolean enabled = true;
}