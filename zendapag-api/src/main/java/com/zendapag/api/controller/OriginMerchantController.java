package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.service.OriginMerchantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API de ORIGEM externa (gateway): registra estabelecimentos marcados com a
 * origem da API Key. Autenticado por X-API-Key (ROLE_ORIGIN), não por JWT.
 *
 * O `source` vem da API Key autenticada (não do corpo) — uma key do ONE_A_ONE só
 * cria estabelecimentos ONE_A_ONE.
 */
@RestController
@RequestMapping("/api/v1/origin")
@PreAuthorize("hasRole('ORIGIN')")
@RequiredArgsConstructor
@Slf4j
public class OriginMerchantController {

    private final OriginMerchantService originMerchantService;

    public static class CreateMerchantRequest {
        @NotBlank @Size(max = 255) public String name;
        @NotBlank @Size(max = 20) public String document;
        @Size(max = 255) public String email;
        @Size(max = 20) public String phone;
        /** Id do estabelecimento no sistema da origem (para mapear os dois lados). */
        @Size(max = 120) public String externalId;
        /** Webhook do estabelecimento (opcional). */
        @Size(max = 500) public String webhookUrl;

        public String getName() { return name; }
        public String getDocument() { return document; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getExternalId() { return externalId; }
        public String getWebhookUrl() { return webhookUrl; }
    }

    @PostMapping("/merchants")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createMerchant(
            @Valid @RequestBody CreateMerchantRequest request,
            Authentication authentication) {
        String source = authentication.getName(); // código da origem (da API Key)
        log.info("[Origin {}] criando estabelecimento {} (externalId {})", source, request.getName(), request.getExternalId());

        Merchant m = originMerchantService.provision(source, request.getName(), request.getDocument(),
            request.getEmail(), request.getPhone(), request.getExternalId(), request.getWebhookUrl());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchantId", m.getId().toString());   // id na Zend — o gateway guarda este
        body.put("source", m.getSource());
        body.put("externalId", m.getSourceExternalId());
        body.put("name", m.getName());
        body.put("document", m.getDocument());
        body.put("status", m.getStatus() != null ? m.getStatus().name() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Estabelecimento registrado", body));
    }
}
