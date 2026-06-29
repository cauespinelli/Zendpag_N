package com.zendapag.core.service.inbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendapag.core.entity.enums.InboundEventType;
import com.zendapag.core.util.HmacUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adapter de webhook de entrada (sandbox): verificação de assinatura HMAC e
 * parsing do payload do PSP para o evento normalizado.
 */
@DisplayName("SandboxInboundProvider — assinatura e parsing do webhook de entrada")
class SandboxInboundProviderTest {

    private static final String SECRET = "whsec_psp_sandbox_dev";
    private final SandboxInboundProvider provider = new SandboxInboundProvider(new ObjectMapper());

    private Map<String, String> sig(String body) {
        return Map.of("x-psp-signature", "sha256=" + HmacUtil.sha256Hex(SECRET, body));
    }

    @Test
    @DisplayName("assinatura correta sobre o corpo → válida")
    void assinaturaValida() {
        String body = "{\"id\":\"evt_1\",\"type\":\"payment.confirmed\",\"reference\":\"PAY-1\"}";
        assertThat(provider.verifySignature(body, sig(body), SECRET)).isTrue();
    }

    @Test
    @DisplayName("assinatura de outro corpo → inválida (não falsificável)")
    void assinaturaDeOutroCorpo() {
        String body = "{\"id\":\"evt_1\",\"type\":\"payment.confirmed\",\"reference\":\"PAY-1\"}";
        Map<String, String> wrong = sig("{\"adulterado\":true}");
        assertThat(provider.verifySignature(body, wrong, SECRET)).isFalse();
    }

    @Test
    @DisplayName("sem header de assinatura → inválida")
    void semAssinatura() {
        assertThat(provider.verifySignature("{}", Map.of(), SECRET)).isFalse();
    }

    @Test
    @DisplayName("segredo errado → inválida")
    void segredoErrado() {
        String body = "{\"id\":\"evt_1\",\"type\":\"payment.confirmed\"}";
        assertThat(provider.verifySignature(body, sig(body), "outro-segredo")).isFalse();
    }

    @Test
    @DisplayName("parse traduz tipos do PSP para o evento normalizado")
    void parseNormaliza() {
        assertThat(provider.parse("{\"id\":\"e\",\"type\":\"payment.confirmed\",\"reference\":\"R\"}").type())
            .isEqualTo(InboundEventType.PAYMENT_CONFIRMED);
        assertThat(provider.parse("{\"id\":\"e\",\"type\":\"card.declined\",\"reference\":\"R\"}").type())
            .isEqualTo(InboundEventType.PAYMENT_FAILED);
        assertThat(provider.parse("{\"id\":\"e\",\"type\":\"boleto.paid\",\"reference\":\"R\"}").type())
            .isEqualTo(InboundEventType.PAYMENT_CONFIRMED);
        var ev = provider.parse("{\"id\":\"evt_9\",\"type\":\"payment.confirmed\",\"reference\":\"PAY-9\"}");
        assertThat(ev.eventId()).isEqualTo("evt_9");
        assertThat(ev.referenceId()).isEqualTo("PAY-9");
    }
}
