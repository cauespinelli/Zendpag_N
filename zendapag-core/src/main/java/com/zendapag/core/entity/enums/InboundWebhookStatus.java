package com.zendapag.core.entity.enums;

/**
 * Status de um webhook RECEBIDO do PSP (entrada).
 * RECEIVED          - registrado, ainda não processado
 * PROCESSED         - processado com sucesso (efeito aplicado)
 * DUPLICATE         - replay de um evento já processado (idempotência) — sem efeito
 * INVALID_SIGNATURE - assinatura ausente/incorreta — rejeitado, sem efeito
 * IGNORED           - evento válido mas sem ação mapeada
 * FAILED            - erro ao processar (passível de reprocessamento)
 */
public enum InboundWebhookStatus {
    RECEIVED,
    PROCESSED,
    DUPLICATE,
    INVALID_SIGNATURE,
    IGNORED,
    FAILED
}
