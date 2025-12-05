package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Webhook;
import com.zendapag.core.entity.enums.WebhookStatus;
import com.zendapag.core.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookRepository webhookRepository;

    public void sendWebhookNotification(String webhookId, String payload) throws BusinessException {
        log.info("Sending webhook notification: {}", webhookId);
    }

    public void retryFailedWebhook(String webhookId) throws BusinessException {
        log.info("Retrying failed webhook: {}", webhookId);
    }

    @Transactional
    public Webhook sendMerchantWebhook(Merchant merchant, String eventType, Map<String, Object> payload) {
        log.info("Sending webhook for merchant: {}, event: {}", merchant.getId(), eventType);
        String webhookUrl = merchant.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            throw new BusinessException("Merchant does not have a webhook URL configured");
        }
        Webhook webhook = new Webhook(merchant, eventType, webhookUrl, payload);
        return webhookRepository.save(webhook);
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
        log.info("Retrying webhook: {}", webhookId);
        Webhook webhook = webhookRepository.findById(webhookId)
            .orElseThrow(() -> new ResourceNotFoundException("Webhook not found: " + webhookId));
        if (!webhook.canRetry()) {
            throw new BusinessException("Webhook cannot be retried");
        }
        webhook.scheduleRetry();
        return webhookRepository.save(webhook);
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

    public record WebhookStatistics(long total, long delivered, long failed, long pending) {}
}
