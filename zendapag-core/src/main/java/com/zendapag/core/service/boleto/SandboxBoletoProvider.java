package com.zendapag.core.service.boleto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Emissor de boleto SANDBOX: gera código de barras, linha digitável (formato
 * plausível) e vencimento de forma DETERMINÍSTICA a partir do referenceId/valor.
 * Substituível pelo emissor real (mesma interface).
 *
 * Os números são FICTÍCIOS (sandbox) — não compõem um boleto bancário válido.
 */
@Component
@Slf4j
public class SandboxBoletoProvider implements BoletoProvider {

    @Override
    public String providerKey() {
        return "sandbox";
    }

    @Override
    public BoletoIssueResult issue(BoletoIssueRequest req) {
        LocalDate dueDate = LocalDate.now().plusDays(Math.max(1, req.dueInDays()));
        long cents = req.amount() != null ? req.amount().movePointRight(2).longValue() : 0L;
        String seed = (req.referenceId() != null ? req.referenceId() : "BOLETO") + ":" + cents;

        String barcode = digits(seed + ":barcode", 44);
        String digitable = formatDigitableLine(digits(seed + ":digitable", 47));
        String url = "https://sandbox.zendpag.com/boleto/" + (req.referenceId() != null ? req.referenceId() : "novo");

        log.info("[SandboxBoleto] emitido {} — venc {}, barcode {}…", req.referenceId(), dueDate, barcode.substring(0, 12));
        return new BoletoIssueResult(barcode, digitable, dueDate, url);
    }

    /** Gera N dígitos determinísticos a partir de um seed (FNV-1a + ressemeadura). */
    private String digits(String seed, int n) {
        StringBuilder sb = new StringBuilder(n);
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < n; i++) {
            for (int c = 0; c < (seed.length() == 0 ? 1 : seed.length()); c++) {
                char ch = seed.isEmpty() ? (char) i : seed.charAt((c + i) % seed.length());
                h ^= ch;
                h *= 0x100000001b3L;
            }
            sb.append((int) Math.floorMod(h, 10));
        }
        return sb.toString();
    }

    /** Formata 47 dígitos no padrão visual da linha digitável (campos separados). */
    private String formatDigitableLine(String d) {
        if (d.length() < 47) {
            return d;
        }
        return d.substring(0, 5) + "." + d.substring(5, 10) + " "
            + d.substring(10, 15) + "." + d.substring(15, 21) + " "
            + d.substring(21, 26) + "." + d.substring(26, 32) + " "
            + d.substring(32, 33) + " " + d.substring(33, 47);
    }
}
