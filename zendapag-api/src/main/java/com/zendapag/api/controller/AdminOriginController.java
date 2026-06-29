package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.Origin;
import com.zendapag.core.service.OriginService;
import com.zendapag.core.service.OriginService.OriginKey;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerência das ORIGENS (ADMIN). Cria a origem e GERA a API Key — a key em texto
 * puro é devolvida UMA vez (na criação/rotação). É assim que o admin gera a key
 * para entregar ao gateway externo.
 */
@RestController
@RequestMapping("/api/v1/admin/origins")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminOriginController {

    private final OriginService originService;

    public static class CreateOriginRequest {
        @NotBlank public String code;
        public String name;
        public String webhookUrl;
        public String webhookSecret;
        public String getCode() { return code; }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        List<Map<String, Object>> out = originService.list().stream().map(this::toMap).toList();
        return ResponseEntity.ok(ApiResponse.success("Origens", out));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody CreateOriginRequest req) {
        OriginKey created = originService.createOrigin(
            req.code, req.name, req.webhookUrl, req.webhookSecret);
        return ResponseEntity.ok(ApiResponse.success("Origem criada — guarde a apiKey (mostrada só agora)",
            withKey(created)));
    }

    @PostMapping("/{code}/rotate-key")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rotate(@PathVariable String code) {
        OriginKey rotated = originService.rotateKey(code);
        return ResponseEntity.ok(ApiResponse.success("API Key rotacionada — guarde a nova apiKey",
            withKey(rotated)));
    }

    private Map<String, Object> withKey(OriginKey k) {
        Map<String, Object> m = toMap(k.origin());
        m.put("apiKey", k.plaintextKey()); // texto puro — mostrado só uma vez
        return m;
    }

    private Map<String, Object> toMap(Origin o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", o.getCode());
        m.put("name", o.getName());
        m.put("apiKeyPrefix", o.getApiKeyPrefix());
        m.put("webhookUrl", o.getWebhookUrl());
        m.put("active", o.isActive());
        return m;
    }
}
