package com.zendapag.core.service.boleto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Adapter de EMISSOR de boleto (banco/PSP). O núcleo fala só com esta interface;
 * trocar o sandbox pelo emissor real é fornecer outro adapter, sem mexer no motor.
 */
public interface BoletoProvider {

    /** Chave do adapter (ex.: "sandbox"). */
    String providerKey();

    /** Emite o boleto e devolve os dados para o pagador. */
    BoletoIssueResult issue(BoletoIssueRequest request);

    record BoletoIssueRequest(
        String referenceId,
        BigDecimal amount,
        int dueInDays,
        String payerName,
        String payerDocument
    ) {}

    /**
     * @param barcode       código de barras numérico
     * @param digitableLine linha digitável (~47 dígitos)
     * @param dueDate       vencimento
     * @param url           link da 2ª via / PDF
     */
    record BoletoIssueResult(String barcode, String digitableLine, LocalDate dueDate, String url) {}
}
