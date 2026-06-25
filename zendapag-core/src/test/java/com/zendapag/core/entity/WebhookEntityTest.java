package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.WebhookStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes da lógica de retry/backoff e transição de status que vive na entidade
 * Webhook (é aqui que mora a regra de reentrega, independente do transporte HTTP).
 */
@DisplayName("Webhook (entity) — retry, backoff e status")
class WebhookEntityTest {

    private Webhook webhook;

    @BeforeEach
    void setUp() {
        Merchant merchant = new Merchant("Loja Teste", "12345678000190", "loja@teste.com");
        webhook = new Webhook(merchant, "PAYMENT_COMPLETED", "https://cliente.com/hook", Map.of("k", "v"));
    }

    @Test
    @DisplayName("nasce PENDING com retryCount 0 e maxRetries 5")
    void estadoInicial() {
        assertThat(webhook.getStatus()).isEqualTo(WebhookStatus.PENDING);
        assertThat(webhook.getRetryCount()).isZero();
        assertThat(webhook.getMaxRetries()).isEqualTo(5);
    }

    @Test
    @DisplayName("markAsFailed marca FAILED, incrementa retryCount e agenda nextRetryAt (~60s)")
    void falhaAgendaRetry() {
        Instant antes = Instant.now();
        webhook.markAsFailed(500, "erro", null, 12L, "HTTP 500");

        assertThat(webhook.getStatus()).isEqualTo(WebhookStatus.FAILED);
        assertThat(webhook.getRetryCount()).isEqualTo(1);
        assertThat(webhook.getNextRetryAt()).isNotNull();
        // 1ª tentativa: 60s * 2^0 = 60s
        assertThat(webhook.getNextRetryAt()).isBetween(antes.plusSeconds(58), antes.plusSeconds(63));
    }

    @Test
    @DisplayName("backoff exponencial: 1ª falha ~60s, 2ª ~120s, 3ª ~240s")
    void backoffExponencial() {
        Instant t1 = Instant.now();
        webhook.markAsFailed(500, null, null, 1L, "f1");
        assertThat(webhook.getNextRetryAt()).isBetween(t1.plusSeconds(58), t1.plusSeconds(63));

        Instant t2 = Instant.now();
        webhook.markAsFailed(500, null, null, 1L, "f2");
        assertThat(webhook.getRetryCount()).isEqualTo(2);
        assertThat(webhook.getNextRetryAt()).isBetween(t2.plusSeconds(118), t2.plusSeconds(123));

        Instant t3 = Instant.now();
        webhook.markAsFailed(500, null, null, 1L, "f3");
        assertThat(webhook.getRetryCount()).isEqualTo(3);
        assertThat(webhook.getNextRetryAt()).isBetween(t3.plusSeconds(238), t3.plusSeconds(243));
    }

    @Test
    @DisplayName("após esgotar maxRetries, canRetry() vira false e retryCount não passa de 5")
    void esgotaRetries() {
        for (int i = 0; i < 7; i++) {
            webhook.markAsFailed(500, null, null, 1L, "falha " + i);
        }
        assertThat(webhook.getRetryCount()).isEqualTo(5); // não ultrapassa maxRetries
        assertThat(webhook.canRetry()).isFalse();
    }

    @Test
    @DisplayName("markAsDelivered registra DELIVERED com status e tempo de resposta")
    void entregaRegistraStatus() {
        webhook.markAsSent();
        assertThat(webhook.getStatus()).isEqualTo(WebhookStatus.SENT);

        webhook.markAsDelivered(200, "OK", null, 45L);
        assertThat(webhook.getStatus()).isEqualTo(WebhookStatus.DELIVERED);
        assertThat(webhook.getResponseStatus()).isEqualTo(200);
        assertThat(webhook.getResponseTimeMs()).isEqualTo(45L);
        assertThat(webhook.getDeliveredAt()).isNotNull();
        assertThat(webhook.isSuccessful()).isTrue();
    }
}
