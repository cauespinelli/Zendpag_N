package com.zendapag.core.service.inbound;

import com.zendapag.core.entity.enums.InboundEventType;

/**
 * Evento de entrada já NORMALIZADO pelo adapter do PSP. O núcleo só lida com
 * isto — não conhece o formato proprietário de cada provedor.
 *
 * @param eventId     ID único do evento no PSP (idempotência)
 * @param type        tipo normalizado
 * @param referenceId referenceId do nosso pagamento/saque
 * @param rawType     tipo original do PSP (auditoria)
 */
public record NormalizedEvent(String eventId, InboundEventType type, String referenceId, String rawType) {}
