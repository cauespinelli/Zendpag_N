package com.zendapag.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Schema(description = "Merchant profile information")
public class MerchantResponse {

    @Schema(description = "Merchant ID")
    private String id;

    @Schema(description = "Document number (CPF/CNPJ)")
    private String document;

    @Schema(description = "Legal name")
    private String name;

    @Schema(description = "Trading name")
    private String tradingName;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Phone number")
    private String phoneNumber;

    @Schema(description = "Website URL")
    private String websiteUrl;

    @Schema(description = "Business description")
    private String description;

    @Schema(description = "Merchant status")
    private String status;

    @Schema(description = "Origem (multi-tenant): DIRETO ou origem externa (ex.: ONE_A_ONE)")
    private String source;

    @Schema(description = "Id do estabelecimento no sistema da origem")
    private String sourceExternalId;

    @Schema(description = "KYC verification status")
    private Boolean kycVerified;

    @Schema(description = "Risk score")
    private Integer riskScore;

    @Schema(description = "Business address")
    private Map<String, Object> address;

    @Schema(description = "Account creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last login timestamp")
    private Instant lastLoginAt;
}