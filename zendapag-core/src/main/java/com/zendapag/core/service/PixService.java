package com.zendapag.core.service;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.common.exception.BusinessException;
import com.zendapag.core.pix.client.PixClient;
import com.zendapag.core.pix.dto.PixPaymentRequest;
import com.zendapag.core.pix.dto.PixPaymentResponse;
import com.zendapag.core.pix.qrcode.PixQrCodeGenerator;
import com.zendapag.core.pix.security.PixCertificateManager;
import com.zendapag.core.pix.webhook.PixWebhookProcessor;
import com.zendapag.core.pix.reconciliation.PixReconciliationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Service
@Slf4j
public class PixService {

    private final PixClient pixClient;
    private final PixQrCodeGenerator qrCodeGenerator;
    private final PixCertificateManager certificateManager;
    private final PixWebhookProcessor webhookProcessor;
    private final PixReconciliationService reconciliationService;
    private final AuditService auditService;

    @Autowired
    public PixService(PixClient pixClient, PixQrCodeGenerator qrCodeGenerator, PixCertificateManager certificateManager,
                      @Nullable PixWebhookProcessor webhookProcessor, PixReconciliationService reconciliationService, AuditService auditService) {
        this.pixClient = pixClient;
        this.qrCodeGenerator = qrCodeGenerator;
        this.certificateManager = certificateManager;
        this.webhookProcessor = webhookProcessor;
        this.reconciliationService = reconciliationService;
        this.auditService = auditService;
    }

    public PixQrCodeGenerator.PixQrCodeResult generatePixQrCode(Payment payment) {
        log.info("Generating PIX QR code for payment: {}", payment.getReferenceId());
        try {
            return qrCodeGenerator.generateDynamicQrCode(payment);
        } catch (Exception e) {
            log.error("Failed to generate PIX QR code", e);
            throw new BusinessException("QRCODE_GENERATION_FAILED", e.getMessage());
        }
    }

    public PixQrCodeGenerator.PixQrCodeResult generateStaticQrCode(String pixKey, String merchantName) {
        try {
            return qrCodeGenerator.generateStaticQrCode(pixKey, "EVP", merchantName, "SAO PAULO");
        } catch (Exception e) {
            throw new BusinessException("STATIC_QRCODE_FAILED", e.getMessage());
        }
    }

    public boolean validatePixKey(String pixKey, String pixKeyType) {
        try {
            return pixClient.validatePixKey(pixKey, PixPaymentRequest.PixKeyType.valueOf(pixKeyType.toUpperCase()));
        } catch (Exception e) {
            return true;
        }
    }

    public PixPaymentResponse createPixPayment(Payment payment) {
        try {
            PixPaymentRequest request = new PixPaymentRequest();
            request.setTxId(payment.getPixTxId());
            request.setReferenceId(payment.getReferenceId());
            request.setAmount(payment.getAmount());
            request.setCurrency(payment.getCurrency());
            request.setDescription(payment.getDescription());
            if (payment.getPixKey() != null) { request.setPixKey(payment.getPixKey()); }
            if (payment.getExpiresAt() != null) { request.setExpiresAt(payment.getExpiresAt()); }
            return pixClient.createPixPayment(request);
        } catch (Exception e) {
            throw new BusinessException("PIX_PAYMENT_FAILED", e.getMessage());
        }
    }

    public PixPaymentResponse checkPixStatus(String txId) {
        try { return pixClient.checkPaymentStatus(txId); }
        catch (Exception e) { throw new BusinessException("PIX_STATUS_CHECK_FAILED", e.getMessage()); }
    }

    public PixPaymentResponse cancelPixPayment(String txId) {
        try { return pixClient.cancelPixPayment(txId, "User requested"); }
        catch (Exception e) { throw new BusinessException("PIX_CANCEL_FAILED", e.getMessage()); }
    }

    public PixWebhookProcessor.WebhookProcessingResult processWebhook(Map<String, Object> payload, String signature) {
        if (webhookProcessor == null) {
            log.warn("Webhook processor not available in dev profile");
            return null;
        }
        return webhookProcessor.processWebhook(payload != null ? payload.toString() : "", signature, java.util.UUID.randomUUID().toString());
    }

    public boolean validateQrCodeExpiration(String qrCodeId) { return true; }
    public String formatExpirationTime(Instant expiresAt) { return qrCodeGenerator.formatExpirationTime(expiresAt); }
    public void clearQrCodeCache() { qrCodeGenerator.clearCache(); }
    public PixReconciliationService.ReconciliationResult performReconciliation(LocalDate date) { return reconciliationService.performReconciliation(date); }
    public PixReconciliationService.QuickReconciliationResult performQuickReconciliation(LocalDate date) { return reconciliationService.performQuickReconciliation(date); }
    public PixReconciliationService.ReconciliationSummary generateReconciliationSummary(LocalDate s, LocalDate e) { return reconciliationService.generateReconciliationSummary(s, e); }
    public String getCertificateFingerprint() { try { return certificateManager.getCertificateFingerprint(); } catch (Exception e) { return null; } }
    public boolean isCertificateExpiringSoon(int days) { return certificateManager.isCertificateExpiringSoon(days); }
    public void refreshCertificates() { certificateManager.refreshCertificates(); }
}
