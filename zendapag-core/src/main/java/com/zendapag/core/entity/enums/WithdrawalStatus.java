package com.zendapag.core.entity.enums;

/**
 * Status do saque PIX
 */
public enum WithdrawalStatus {
    /**
     * Saque solicitado, aguardando processamento
     */
    PENDING,

    /**
     * Saque em processamento
     */
    PROCESSING,

    /**
     * Saque aprovado, aguardando transferência
     */
    APPROVED,

    /**
     * Saque concluído com sucesso
     */
    COMPLETED,

    /**
     * Saque rejeitado
     */
    REJECTED,

    /**
     * Saque cancelado
     */
    CANCELLED,

    /**
     * Saque falhou durante processamento
     */
    FAILED,

    /**
     * Saque estornado
     */
    REVERSED
}
