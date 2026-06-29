package com.zendapag.core.service.inbound;

import java.util.Map;

/**
 * Adapter de um PSP para webhooks de ENTRADA. Cada provedor implementa a sua
 * verificação de assinatura e o parsing do seu formato proprietário para um
 * NormalizedEvent. Adicionar um PSP = um novo bean, sem tocar no núcleo.
 */
public interface InboundWebhookProvider {

    /** Chave do provider, casada com o {provider} da URL (ex.: "sandbox"). */
    String providerKey();

    /**
     * Verifica que o webhook veio mesmo do PSP. DEVE rejeitar corpo sem assinatura,
     * com assinatura malformada ou que não bate com o HMAC do segredo compartilhado.
     */
    boolean verifySignature(String rawBody, Map<String, String> headers, String secret);

    /** Traduz o corpo cru para um evento normalizado. Só é chamado após assinatura válida. */
    NormalizedEvent parse(String rawBody);
}
