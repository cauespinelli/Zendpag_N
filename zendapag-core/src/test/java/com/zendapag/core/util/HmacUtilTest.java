package com.zendapag.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes da assinatura HMAC-SHA256 usada para assinar os webhooks. É o que
 * permite ao cliente validar a autenticidade do POST recebido.
 */
@DisplayName("HmacUtil — assinatura HMAC-SHA256 dos webhooks")
class HmacUtilTest {

    @Test
    @DisplayName("vetor de teste canônico: HMAC-SHA256(key, fox) bate com o hash esperado")
    void vetorCanonico() {
        // Vetor público conhecido de HMAC-SHA256.
        String mac = HmacUtil.sha256Hex("key", "The quick brown fox jumps over the lazy dog");
        assertThat(mac).isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    @DisplayName("é determinístico: mesma entrada → mesma assinatura")
    void deterministico() {
        String a = HmacUtil.sha256Hex("whsec_dev_123", "{\"event\":\"PAYMENT_COMPLETED\"}");
        String b = HmacUtil.sha256Hex("whsec_dev_123", "{\"event\":\"PAYMENT_COMPLETED\"}");
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("segredo diferente → assinatura diferente (não falsificável sem o segredo)")
    void segredoDiferenteAssinaturaDiferente() {
        String body = "{\"event\":\"PAYMENT_COMPLETED\",\"amount\":100}";
        String comSegredoCerto = HmacUtil.sha256Hex("segredo-correto", body);
        String comSegredoErrado = HmacUtil.sha256Hex("segredo-errado", body);
        assertThat(comSegredoCerto).isNotEqualTo(comSegredoErrado);
    }

    @Test
    @DisplayName("corpo diferente → assinatura diferente (detecta adulteração)")
    void corpoDiferenteAssinaturaDiferente() {
        String s1 = HmacUtil.sha256Hex("segredo", "{\"amount\":100}");
        String s2 = HmacUtil.sha256Hex("segredo", "{\"amount\":999}");
        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    @DisplayName("saída é hex de 64 caracteres (256 bits)")
    void formatoHex64() {
        String mac = HmacUtil.sha256Hex("segredo", "qualquer-corpo");
        assertThat(mac).hasSize(64).matches("[0-9a-f]{64}");
    }
}
