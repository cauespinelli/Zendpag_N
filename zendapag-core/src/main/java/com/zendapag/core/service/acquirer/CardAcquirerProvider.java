package com.zendapag.core.service.acquirer;

import java.math.BigDecimal;

/**
 * Adapter de ADQUIRENTE para cobrança com cartão. O núcleo fala só com esta
 * interface; trocar o sandbox pela adquirente real (Cielo/Stone/etc.) é fornecer
 * outro adapter, sem mexer no motor.
 *
 * IMPORTANTE (PCI): a autorização recebe um TOKEN do cartão (tokenização feita
 * pelo parceiro), nunca PAN/CVV. Guardamos apenas token, bandeira e últimos 4.
 */
public interface CardAcquirerProvider {

    /** Chave do adapter (ex.: "sandbox"). */
    String providerKey();

    /** Solicita a autorização da cobrança à adquirente. */
    CardAuthorizationResult authorize(CardAuthorizationRequest request);

    /** Pedido de autorização (dados tokenizados, valor, parcelas). */
    record CardAuthorizationRequest(
        String referenceId,
        String cardToken,
        String brand,
        String lastFour,
        BigDecimal amount,
        int installments
    ) {}

    /** Status do 3-D Secure devolvido pela adquirente. */
    enum ThreeDsOutcome { NOT_REQUIRED, CHALLENGE_REQUIRED, AUTHENTICATED }

    /**
     * Resultado da autorização.
     * @param approved        autorizado (true) ou recusado (false)
     * @param threeDs         desfecho do 3DS
     * @param authorizationCode código de autorização (quando aprovado)
     * @param nsu             NSU/identificador da adquirente
     * @param challengeId     id do desafio 3DS (quando CHALLENGE_REQUIRED)
     * @param declineReason   motivo da recusa (quando recusado)
     */
    record CardAuthorizationResult(
        boolean approved,
        ThreeDsOutcome threeDs,
        String authorizationCode,
        String nsu,
        String challengeId,
        String declineReason
    ) {
        public static CardAuthorizationResult approved(String authCode, String nsu, ThreeDsOutcome threeDs) {
            return new CardAuthorizationResult(true, threeDs, authCode, nsu, null, null);
        }
        public static CardAuthorizationResult challenge(String challengeId) {
            return new CardAuthorizationResult(false, ThreeDsOutcome.CHALLENGE_REQUIRED, null, null, challengeId, null);
        }
        public static CardAuthorizationResult declined(String reason) {
            return new CardAuthorizationResult(false, ThreeDsOutcome.NOT_REQUIRED, null, null, null, reason);
        }
    }
}
