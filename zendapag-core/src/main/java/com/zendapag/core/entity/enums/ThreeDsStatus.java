package com.zendapag.core.entity.enums;

/**
 * Status da autenticação 3-D Secure (autenticação do portador) de um pagamento
 * com cartão.
 *
 * NOT_REQUIRED  - fluxo sem 3DS (ex.: PIX/boleto) ou frictionless dispensado
 * REQUIRED      - desafio 3DS pendente (aguardando o portador autenticar)
 * AUTHENTICATED - portador autenticado (frictionless ou desafio concluído)
 * FAILED        - autenticação 3DS falhou/recusada
 */
public enum ThreeDsStatus {
    NOT_REQUIRED,
    REQUIRED,
    AUTHENTICATED,
    FAILED
}
