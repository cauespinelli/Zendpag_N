package com.zendapag.api.scheduler;

import com.zendapag.core.service.BalanceReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job de liberação de saldo: periodicamente move o líquido retido de PENDENTE
 * para DISPONÍVEL quando o vencimento (availableAt) chega. Sem este scan, o
 * saldo pendente nunca viraria disponível automaticamente.
 *
 * Intervalo padrão: 30s (sobrescrevível por property).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceReleaseScheduler {

    private final BalanceReleaseService balanceReleaseService;

    @Scheduled(
        fixedDelayString = "${zendapag.payout.release-scan-ms:30000}",
        initialDelayString = "${zendapag.payout.release-initial-delay-ms:15000}")
    public void releaseDuePending() {
        try {
            int n = balanceReleaseService.releaseDuePending();
            if (n > 0) {
                log.info("BalanceReleaseScheduler: {} lançamento(s) liberado(s) pendente -> disponível.", n);
            }
        } catch (Exception e) {
            log.warn("BalanceReleaseScheduler: falha ao liberar saldos devidos: {}", e.getMessage());
        }
    }
}
