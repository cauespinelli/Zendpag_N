package com.zendapag.api.scheduler;

import com.zendapag.core.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reprocessa periodicamente os webhooks FAILED cujo nextRetryAt já venceu
 * (backoff exponencial calculado na entidade Webhook). Sem este job, o retry
 * de webhooks entregues por esta aplicação só aconteceria por gatilho manual.
 *
 * Intervalo padrão: 60s (sobrescrevível por property). O próprio WebhookService
 * respeita o agendamento de cada webhook, então este scan apenas "acorda" os
 * que já estão devidos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryScheduler {

    private final WebhookService webhookService;

    @Scheduled(
        fixedDelayString = "${zendapag.webhooks.retry-scan-ms:60000}",
        initialDelayString = "${zendapag.webhooks.retry-initial-delay-ms:60000}")
    public void retryDueWebhooks() {
        try {
            int n = webhookService.retryDueWebhooks();
            if (n > 0) {
                log.info("WebhookRetryScheduler: {} webhook(s) reprocessado(s).", n);
            }
        } catch (Exception e) {
            log.warn("WebhookRetryScheduler: falha ao reprocessar webhooks devidos: {}", e.getMessage());
        }
    }
}
