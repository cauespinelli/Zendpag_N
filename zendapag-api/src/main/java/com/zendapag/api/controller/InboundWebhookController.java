package com.zendapag.api.controller;

import com.zendapag.core.entity.enums.InboundWebhookStatus;
import com.zendapag.core.service.InboundWebhookService;
import com.zendapag.core.service.InboundWebhookService.ReceiveResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Receptor de webhooks de ENTRADA do PSP (PSP -> nós).
 *
 * POST /api/v1/webhooks/psp/{provider}
 *
 * Aberto na segurança (permitAll) porque o PSP não tem JWT — a autenticação é a
 * ASSINATURA HMAC validada pelo adapter. Corpo lido cru (a assinatura cobre os
 * bytes exatos recebidos).
 *
 * HTTP:
 *   200 - processado / duplicado / ignorado (ack: não reenviar)
 *   401 - assinatura inválida (rejeitado, sem efeito)
 *   422 - falha de processamento (registrado; reprocessável pelo admin)
 *   404 - provider desconhecido (via handler de ResourceNotFound)
 */
@RestController
@RequestMapping("/api/v1/webhooks/psp")
@RequiredArgsConstructor
@Slf4j
public class InboundWebhookController {

    private final InboundWebhookService inboundWebhookService;

    @PostMapping("/{provider}")
    public ResponseEntity<Map<String, Object>> receive(
            @PathVariable String provider,
            @RequestBody(required = false) String body,
            @RequestHeader Map<String, String> headers) {

        ReceiveResult result = inboundWebhookService.receive(provider, body != null ? body : "", headers);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", result.status().name());
        resp.put("message", result.message());
        resp.put("id", result.id());

        HttpStatus http = switch (result.status()) {
            case INVALID_SIGNATURE -> HttpStatus.UNAUTHORIZED;
            case FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.OK; // PROCESSED, DUPLICATE, IGNORED, RECEIVED
        };
        return ResponseEntity.status(http).body(resp);
    }
}
