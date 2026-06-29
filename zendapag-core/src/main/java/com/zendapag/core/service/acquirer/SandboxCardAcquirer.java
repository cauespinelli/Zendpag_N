package com.zendapag.core.service.acquirer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adquirente SANDBOX: decide aprovação/recusa/3DS de forma DETERMINÍSTICA pelo
 * token do cartão (nunca por PAN). Convenção de teste:
 *
 *   token contém "decline" -> RECUSADO
 *   token contém "3ds"     -> exige desafio 3DS (CHALLENGE_REQUIRED)
 *   caso contrário          -> APROVADO (frictionless)
 *
 * Substituível pelo adapter da adquirente real (mesma interface).
 */
@Component
@Slf4j
public class SandboxCardAcquirer implements CardAcquirerProvider {

    @Override
    public String providerKey() {
        return "sandbox";
    }

    @Override
    public CardAuthorizationResult authorize(CardAuthorizationRequest req) {
        String token = req.cardToken() != null ? req.cardToken().toLowerCase() : "";

        if (token.contains("decline")) {
            log.info("[SandboxAcquirer] {} RECUSADO (token de teste) — {}x", req.referenceId(), req.installments());
            return CardAuthorizationResult.declined("Cartão recusado pelo emissor (sandbox)");
        }
        if (token.contains("3ds")) {
            String challengeId = "chl_" + UUID.randomUUID().toString().substring(0, 12);
            log.info("[SandboxAcquirer] {} exige 3DS (challenge {})", req.referenceId(), challengeId);
            return CardAuthorizationResult.challenge(challengeId);
        }
        String authCode = "AUTH" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String nsu = "NSU" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        log.info("[SandboxAcquirer] {} APROVADO {}x (auth {}, nsu {})",
            req.referenceId(), req.installments(), authCode, nsu);
        return CardAuthorizationResult.approved(authCode, nsu, ThreeDsOutcome.NOT_REQUIRED);
    }
}
