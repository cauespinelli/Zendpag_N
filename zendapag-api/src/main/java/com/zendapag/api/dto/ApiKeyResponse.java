package com.zendapag.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Schema(description = "API key information")
public class ApiKeyResponse {

    @Schema(description = "API key ID")
    private String keyId;

    @Schema(description = "API key value (only shown once)")
    private String apiKey;

    @Schema(description = "API key description")
    private String description;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Expiration timestamp")
    private Instant expiresAt;
}