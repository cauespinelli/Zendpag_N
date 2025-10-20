package com.zendapag.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Schema(description = "Webhook event information")
public class WebhookEventResponse {

    @Schema(description = "Webhook event ID")
    private String id;

    @Schema(description = "Event type")
    private String eventType;

    @Schema(description = "Webhook URL")
    private String url;

    @Schema(description = "Delivery status")
    private String status;

    @Schema(description = "Number of delivery attempts")
    private Integer attempts;

    @Schema(description = "HTTP response status code")
    private Integer responseStatusCode;

    @Schema(description = "Response time in milliseconds")
    private Long responseTimeMs;

    @Schema(description = "Failure reason (if failed)")
    private String failureReason;

    @Schema(description = "Event creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last delivery attempt timestamp")
    private Instant lastAttemptAt;

    @Schema(description = "Successful delivery timestamp")
    private Instant deliveredAt;

    @Schema(description = "Related payment reference ID")
    private String paymentReferenceId;
}