package com.zendapag.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Webhook;
import com.zendapag.core.entity.enums.WebhookStatus;
import com.zendapag.core.repository.WebhookRepository;
import com.zendapag.core.util.HmacUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Teste de entrega real do webhook: sobe um servidor HTTP in-process que recebe
 * o POST, e valida que (a) o POST chega assinado com HMAC correto sobre o corpo
 * enviado, e (b) o status de entrega é registrado conforme a resposta (2xx →
 * DELIVERED, 5xx → FAILED com retry agendado).
 *
 * Como o HttpClient do WebhookService é estático (não injetável), usamos um
 * servidor local de verdade em vez de mockar o transporte.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService — entrega HTTP real + HMAC")
class WebhookServiceDeliveryTest {

    private static final String SECRET = "whsec_test_123";

    @Mock private WebhookRepository webhookRepository;

    private WebhookService webhookService;
    private HttpServer server;
    private int port;

    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedSignature = new AtomicReference<>();
    private final AtomicReference<String> receivedEvent = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(webhookRepository.save(org.mockito.ArgumentMatchers.any(Webhook.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        webhookService = new WebhookService(webhookRepository, new ObjectMapper());

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/hook", exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                receivedBody.set(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            receivedSignature.set(exchange.getRequestHeaders().getFirst("X-Zendapag-Signature"));
            receivedEvent.set(exchange.getRequestHeaders().getFirst("X-Zendapag-Event"));
            byte[] resp = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus.get(), resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private Webhook newWebhook() {
        Merchant merchant = new Merchant("Loja Teste", "12345678000190", "loja@teste.com");
        merchant.setId(UUID.randomUUID());
        merchant.setWebhookSecret(SECRET);
        Webhook w = new Webhook(merchant, "PAYMENT_COMPLETED", "http://localhost:" + port + "/hook",
            Map.of("event", "PAYMENT_COMPLETED", "amount", 100));
        w.setId(UUID.randomUUID());
        return w;
    }

    @Test
    @DisplayName("entrega 200 → DELIVERED e o POST chega assinado com HMAC válido sobre o corpo")
    void entrega200AssinaCorretamente() {
        responseStatus.set(200);
        Webhook w = newWebhook();

        webhookService.deliver(w);

        // status de entrega registrado
        assertThat(w.getStatus()).isEqualTo(WebhookStatus.DELIVERED);
        assertThat(w.getResponseStatus()).isEqualTo(200);

        // o servidor recebeu o evento e a assinatura
        assertThat(receivedEvent.get()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(receivedSignature.get()).isNotNull();

        // a assinatura recebida valida com o segredo sobre o corpo EXATO recebido
        String esperado = "sha256=" + HmacUtil.sha256Hex(SECRET, receivedBody.get());
        assertThat(receivedSignature.get()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("entrega 500 → FAILED com retryCount 1 e nextRetryAt agendado")
    void entrega500AgendaRetry() {
        responseStatus.set(500);
        Webhook w = newWebhook();

        webhookService.deliver(w);

        assertThat(w.getStatus()).isEqualTo(WebhookStatus.FAILED);
        assertThat(w.getResponseStatus()).isEqualTo(500);
        assertThat(w.getRetryCount()).isEqualTo(1);
        assertThat(w.getNextRetryAt()).isNotNull();
    }
}
