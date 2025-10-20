package com.zendapag.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Merchant profile update request")
public class MerchantUpdateRequest {

    @Schema(description = "Legal name", example = "Empresa XYZ Ltda")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @Schema(description = "Trading name", example = "XYZ Store")
    @Size(max = 255, message = "Trading name must be at most 255 characters")
    private String tradingName;

    @Schema(description = "Email address", example = "contato@empresa.com")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Schema(description = "Phone number", example = "+5511999999999")
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;

    @Schema(description = "Website URL", example = "https://www.empresa.com")
    @Size(max = 500, message = "Website URL must be at most 500 characters")
    private String websiteUrl;

    @Schema(description = "Business description", example = "Online retail store")
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @Schema(description = "Business address information")
    @Valid
    private Map<String, Object> address;
}