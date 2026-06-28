package com.zendapag.core.entity.enums;

/**
 * Origem de um saque PIX.
 * MANUAL    = solicitado por uma pessoa (painel/API do merchant).
 * AUTOMATIC = disparado pelo motor (auto-payout) ao liberar saldo disponível.
 */
public enum WithdrawalTriggerType {
    MANUAL,
    AUTOMATIC
}
