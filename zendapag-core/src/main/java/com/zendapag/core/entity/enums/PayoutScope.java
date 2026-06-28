package com.zendapag.core.entity.enums;

/**
 * Escopo de uma regra de liquidação/saque automático.
 * GLOBAL  = padrão da plataforma (uma linha por método).
 * MERCHANT = override de um estabelecimento específico.
 */
public enum PayoutScope {
    GLOBAL,
    MERCHANT
}
