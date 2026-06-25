package com.zendapag.core.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Assinatura HMAC-SHA256 para webhooks. O destinatário recalcula o HMAC sobre o
 * corpo recebido usando o segredo compartilhado (merchant.webhookSecret) e
 * compara com o header X-Zendapag-Signature.
 */
public final class HmacUtil {

    private HmacUtil() {}

    public static String sha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular HMAC-SHA256", e);
        }
    }
}
