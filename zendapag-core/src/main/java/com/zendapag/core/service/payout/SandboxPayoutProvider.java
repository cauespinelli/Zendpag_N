package com.zendapag.core.service.payout;

import com.zendapag.core.entity.PixWithdrawal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Provedor de saque PIX em SANDBOX: simula o envio com sucesso e devolve um
 * endToEndId sintético. Substituível pelo adapter do PSP real (mesma interface)
 * quando houver contrato.
 */
@Component
@Slf4j
public class SandboxPayoutProvider implements PayoutProvider {

    @Override
    public PayoutResult send(PixWithdrawal w) {
        // Convenção de teste (sandbox): referenceId contendo "FAIL" simula recusa do PSP.
        String ref = w.getReferenceId() != null ? w.getReferenceId().toUpperCase() : "";
        if (ref.contains("FAIL")) {
            log.info("[SandboxPayout] enviando PIX ref={} -> simulado RECUSADO (PSP)", w.getReferenceId());
            return PayoutResult.fail("PIX recusado pelo PSP (sandbox)");
        }
        String e2e = "E2E" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        log.info("[SandboxPayout] enviando PIX ref={} valor={} chave={} ({}) -> simulado OK, e2e={}",
            w.getReferenceId(), w.getAmount(), w.getPixKey(), w.getPixKeyType(), e2e);
        return PayoutResult.ok(e2e);
    }
}
