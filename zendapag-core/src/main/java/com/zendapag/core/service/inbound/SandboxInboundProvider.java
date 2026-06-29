package com.zendapag.core.service.inbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendapag.core.entity.enums.InboundEventType;
import com.zendapag.core.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter SANDBOX de webhook de entrada (simula um PSP) — formato de teste:
 *
 *   POST /api/v1/webhooks/psp/sandbox
 *   Header: X-PSP-Signature: sha256=<hex HMAC-SHA256(secret, corpo cru)>
 *   Body:   {"id":"evt_123","type":"payment.confirmed","reference":"PAY-...","amount":89.90}
 *
 * Substituível por adapters de PSPs reais (Stripe/Adyen/etc.), cada um com seu
 * esquema de assinatura e payload. Comparação de assinatura em tempo constante.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SandboxInboundProvider implements InboundWebhookProvider {

    private static final String SIGNATURE_HEADER = "x-psp-signature";

    private final ObjectMapper objectMapper;

    @Override
    public String providerKey() {
        return "sandbox";
    }

    @Override
    public boolean verifySignature(String rawBody, Map<String, String> headers, String secret) {
        if (secret == null || secret.isBlank() || rawBody == null) {
            return false;
        }
        String provided = headers.get(SIGNATURE_HEADER);
        if (provided == null || provided.isBlank()) {
            log.warn("[InboundSandbox] webhook sem header de assinatura — rejeitado");
            return false;
        }
        String expected = "sha256=" + HmacUtil.sha256Hex(secret, rawBody);
        boolean ok = constantTimeEquals(expected, provided.trim());
        if (!ok) {
            log.warn("[InboundSandbox] assinatura inválida — rejeitado");
        }
        return ok;
    }

    @Override
    public NormalizedEvent parse(String rawBody) {
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            String eventId = text(node, "id");
            String rawType = text(node, "type");
            String reference = text(node, "reference");
            return new NormalizedEvent(eventId, mapType(rawType), reference, rawType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Corpo do webhook inválido: " + e.getMessage(), e);
        }
    }

    private InboundEventType mapType(String rawType) {
        if (rawType == null) return InboundEventType.UNKNOWN;
        return switch (rawType.toLowerCase()) {
            case "payment.confirmed", "payment.approved", "payment.paid",
                 "card.authorized", "card.captured",
                 "boleto.paid", "bankslip.paid" -> InboundEventType.PAYMENT_CONFIRMED;
            case "payment.failed", "payment.refused", "payment.declined",
                 "card.declined", "boleto.expired" -> InboundEventType.PAYMENT_FAILED;
            case "withdrawal.completed", "payout.completed" -> InboundEventType.WITHDRAWAL_COMPLETED;
            default -> InboundEventType.UNKNOWN;
        };
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    /** Comparação em tempo constante para não vazar info por timing. */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
