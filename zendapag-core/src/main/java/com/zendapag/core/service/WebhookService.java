package com.zendapag.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Origin;
import com.zendapag.core.entity.Webhook;
import com.zendapag.core.entity.enums.WebhookStatus;
import com.zendapag.core.repository.OriginRepository;
import com.zendapag.core.repository.WebhookRepository;
import com.zendapag.core.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private final WebhookRepository webhookRepository;
    private final OriginRepository originRepository;
    private final ObjectMapper objectMapper;

    /**
     * Cria o webhook para o merchant e TENTA entregar imediatamente (HTTP POST
     * real + HMAC). Falhas agendam retry (não derrubam o chamador).
     */
    @Transactional
    public Webhook sendMerchantWebhook(Merchant merchant, String eventType, Map<String, Object> payload) {
        String webhookUrl = merchant.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            throw new BusinessException("Merchant does not have a webhook URL configured");
        }
        Webhook webhook = new Webhook(merchant, eventType, webhookUrl, payload);
        webhook = webhookRepository.save(webhook); // persiste p/ ter id
        deliver(webhook);
        return webhook;
    }

    /**
     * Dispara um evento de webhook para o estabelecimento. Nunca lança: se não
     * houver URL ou a entrega falhar, apenas registra (a falha agenda retry).
     * Ponto único usado por pagamento (pago/falhou/estornado) e saque.
     */
    public void notifyMerchant(Merchant merchant, String eventType, Map<String, Object> payload) {
        try {
            if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().isEmpty()) {
                log.info("Merchant {} sem webhookUrl — evento {} não enviado.", merchant.getId(), eventType);
                return;
            }
            sendMerchantWebhook(merchant, eventType, payload);
        } catch (Exception e) {
            log.warn("Falha ao enviar webhook {} para merchant {}: {}", eventType, merchant.getId(), e.getMessage());
        }
    }

    /**
     * Dispara um webhook DE VOLTA para a origem (gateway) do estabelecimento.
     * No-op para a origem interna (DIRETO) ou se a origem não tiver webhookUrl.
     * Assina com o segredo da origem. Nunca lança.
     */
    public void notifyOrigin(Merchant merchant, String eventType, Map<String, Object> payload) {
        try {
            String source = merchant.getSource();
            if (source == null || OriginService.SOURCE_DIRETO.equals(source)) {
                return; // estabelecimento próprio — sem webhook de volta
            }
            Origin origin = originRepository.findByCode(source).orElse(null);
            if (origin == null || origin.getWebhookUrl() == null || origin.getWebhookUrl().isEmpty()) {
                log.info("Origem {} sem webhookUrl — evento {} não enviado de volta.", source, eventType);
                return;
            }
            Webhook webhook = new Webhook(merchant, eventType, origin.getWebhookUrl(), payload);
            webhook.setTargetSource(source);
            webhook = webhookRepository.save(webhook);
            deliver(webhook);
        } catch (Exception e) {
            log.warn("Falha ao enviar webhook {} de volta à origem do merchant {}: {}",
                eventType, merchant.getId(), e.getMessage());
        }
    }

    /** Entrega de fato um webhook (HTTP POST assinado). Atualiza status e agenda retry em falha. */
    @Transactional
    public void deliver(Webhook webhook) {
        long start = System.currentTimeMillis();
        try {
            String body = objectMapper.writeValueAsString(
                webhook.getPayload() != null ? webhook.getPayload() : Map.of());

            // Webhook de ORIGEM assina com o segredo da origem; senão, o do merchant.
            String secret;
            if (webhook.getTargetSource() != null) {
                secret = originRepository.findByCode(webhook.getTargetSource())
                    .map(Origin::getWebhookSecret).orElse(null);
            } else {
                secret = webhook.getMerchant().getWebhookSecret();
            }
            String signature = "sha256=" + (secret != null ? HmacUtil.sha256Hex(secret, body) : "");
            webhook.setSignature(signature);
            webhook.addHeader("Content-Type", "application/json");
            webhook.addHeader("X-Zendapag-Event", webhook.getEventType());
            webhook.addHeader("X-Zendapag-Signature", signature);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhook.getUrl()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Zendapag-Event", webhook.getEventType())
                .header("X-Zendapag-Webhook-Id", webhook.getId() != null ? webhook.getId().toString() : "")
                .header("X-Zendapag-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            long ms = System.currentTimeMillis() - start;
            int sc = response.statusCode();

            if (sc >= 200 && sc < 300) {
                webhook.markAsSent();
                webhook.markAsDelivered(sc, truncate(response.body()), null, ms);
                log.info("Webhook {} ENTREGUE -> {} HTTP {} ({}ms)", webhook.getEventType(), webhook.getUrl(), sc, ms);
            } else {
                webhook.markAsFailed(sc, truncate(response.body()), null, ms, "HTTP " + sc);
                log.warn("Webhook {} FALHOU -> {} HTTP {} (retry #{} agendado p/ {})",
                    webhook.getEventType(), webhook.getUrl(), sc, webhook.getRetryCount(), webhook.getNextRetryAt());
            }
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - start;
            webhook.markAsFailed(null, null, null, ms, "Erro de conexão: " + e.getMessage());
            webhook.setErrorCode("CONNECTION_ERROR");
            log.warn("Webhook {} ERRO de conexão -> {}: {} (retry #{} agendado p/ {})",
                webhook.getEventType(), webhook.getUrl(), e.getMessage(), webhook.getRetryCount(), webhook.getNextRetryAt());
        }
        webhookRepository.save(webhook);
    }

    /** Reprocessa os webhooks FAILED cujo nextRetryAt já venceu (backoff exponencial na entidade). */
    @Transactional
    public int retryDueWebhooks() {
        List<Webhook> due = webhookRepository.findFailedWebhooksReadyForRetry(Instant.now());
        due.forEach(this::deliver);
        log.info("Retry de webhooks: {} reprocessados.", due.size());
        return due.size();
    }

    /** Reentrega forçada (uso manual/dev), ignorando o agendamento. */
    @Transactional
    public Webhook forceRetry(UUID id) {
        Webhook webhook = webhookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Webhook not found: " + id));
        deliver(webhook);
        return webhook;
    }

    /** Entrega um webhook já persistido pelo id. */
    @Transactional
    public void sendWebhookNotification(String webhookId, String payload) throws BusinessException {
        Webhook webhook = webhookRepository.findById(UUID.fromString(webhookId))
            .orElseThrow(() -> new ResourceNotFoundException("Webhook not found: " + webhookId));
        deliver(webhook);
    }

    @Transactional(readOnly = true)
    public Page<Webhook> findByMerchant(Merchant merchant, PageRequest pageRequest) {
        return webhookRepository.findByMerchant(merchant, pageRequest);
    }

    @Transactional(readOnly = true)
    public Optional<Webhook> findById(UUID id) {
        return webhookRepository.findById(id);
    }

    @Transactional
    public Webhook retryWebhook(UUID webhookId) {
        Webhook webhook = webhookRepository.findById(webhookId)
            .orElseThrow(() -> new ResourceNotFoundException("Webhook not found: " + webhookId));
        if (!webhook.canRetry()) {
            throw new BusinessException("Webhook cannot be retried");
        }
        deliver(webhook);
        return webhook;
    }

    @Transactional(readOnly = true)
    public WebhookStatistics getStatistics(Merchant merchant) {
        long delivered = webhookRepository.countByMerchantAndStatus(merchant, WebhookStatus.DELIVERED);
        long failed = webhookRepository.countByMerchantAndStatus(merchant, WebhookStatus.FAILED);
        long pending = webhookRepository.countByMerchantAndStatus(merchant, WebhookStatus.PENDING);
        long sent = webhookRepository.countByMerchantAndStatus(merchant, WebhookStatus.SENT);
        long total = delivered + failed + pending + sent;
        return new WebhookStatistics(total, delivered, failed, pending);
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 900 ? s.substring(0, 900) : s;
    }

    public record WebhookStatistics(long total, long delivered, long failed, long pending) {}
}
