package com.zendapag.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Webhook;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.WebhookRepository;
import com.zendapag.core.service.WebhookService;
import com.zendapag.core.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Receptor de webhook para DESENVOLVIMENTO. Recebe o POST do WebhookService,
 * recalcula o HMAC sobre o corpo recebido com o segredo do estabelecimento e
 * confere com o header X-Zendapag-Signature — fechando o loop de teste do
 * webhook real (HTTP + HMAC) sem depender de serviço externo.
 *
 * Sob /api/v1/webhooks/receive/** (já permitAll na segurança). Só no perfil dev.
 */
@RestController
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevWebhookSinkController {

    private final MerchantRepository merchantRepository;
    private final WebhookRepository webhookRepository;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/api/v1/webhooks/receive/dev-sink")
    public ResponseEntity<String> sink(
            @RequestBody String body,
            @RequestHeader(value = "X-Zendapag-Signature", required = false) String signature,
            @RequestHeader(value = "X-Zendapag-Event", required = false) String event) {
        String verdict = "sem merchant_id";
        try {
            JsonNode node = objectMapper.readTree(body);
            String merchantId = node.path("merchant_id").asText(null);
            if (merchantId != null) {
                Merchant m = merchantRepository.findById(UUID.fromString(merchantId)).orElse(null);
                if (m != null && m.getWebhookSecret() != null) {
                    String expected = "sha256=" + HmacUtil.sha256Hex(m.getWebhookSecret(), body);
                    verdict = expected.equals(signature) ? "HMAC VALIDO" : "HMAC INVALIDO";
                } else {
                    verdict = "merchant nao encontrado / sem secret";
                }
            }
            log.warn("[DevWebhookSink] recebido evento={} merchant={} assinatura={} -> {}",
                event, merchantId, signature, verdict);
        } catch (Exception e) {
            log.warn("[DevWebhookSink] erro ao processar corpo: {}", e.getMessage());
        }
        return ResponseEntity.ok("{\"received\":true,\"hmac\":\"" + verdict + "\"}");
    }

    /** Reprocessa webhooks FAILED devidos (respeita o nextRetryAt). */
    @PostMapping("/api/v1/webhooks/receive/dev-retry-due")
    public ResponseEntity<String> retryDue() {
        int n = webhookService.retryDueWebhooks();
        return ResponseEntity.ok("{\"retried\":" + n + "}");
    }

    /** Reentrega forçada de um webhook (ignora o agendamento) — para teste. */
    @PostMapping("/api/v1/webhooks/receive/dev-force-retry/{id}")
    public ResponseEntity<String> forceRetry(@PathVariable UUID id) {
        Webhook w = webhookService.forceRetry(id);
        return ResponseEntity.ok("{\"status\":\"" + w.getStatus() + "\",\"retryCount\":" + w.getRetryCount() + "}");
    }

    /** Lista webhooks (dev) para inspeção/teste. POST para casar com o permitAll de /receive/**. */
    @PostMapping("/api/v1/webhooks/receive/dev-list")
    public ResponseEntity<List<Map<String, Object>>> list() {
        List<Map<String, Object>> all = webhookRepository.findAll().stream().map(w -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId().toString());
            m.put("event", w.getEventType());
            m.put("status", w.getStatus().name());
            m.put("url", w.getUrl());
            m.put("responseStatus", w.getResponseStatus());
            m.put("retryCount", w.getRetryCount());
            m.put("nextRetryAt", w.getNextRetryAt());
            return m;
        }).toList();
        return ResponseEntity.ok(all);
    }
}
