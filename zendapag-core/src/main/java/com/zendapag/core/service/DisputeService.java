package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Dispute;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.repository.DisputeRepository;
import com.zendapag.core.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Disputas / chargebacks. No modelo real, a abertura de uma disputa chega como
 * notificação do PSP/adquirente; em sandbox, a abertura é disparada pelo Admin
 * (endpoint) simulando essa notificação.
 *
 * A abertura registra a {@link Dispute} (status OPENED) sobre um pagamento
 * APROVADO e dispara o webhook DISPUTE_CREATED para o estabelecimento, para que
 * ele saiba que precisa apresentar defesa/evidência.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final PaymentRepository paymentRepository;
    private final WebhookService webhookService;

    /**
     * Abre uma disputa (chargeback) sobre um pagamento aprovado e notifica o
     * estabelecimento via webhook DISPUTE_CREATED.
     *
     * @param paymentId   pagamento contestado (deve estar APROVADO)
     * @param reasonCode  motivo (ver {@link com.zendapag.core.entity.enums.DisputeReason}); default FRAUD
     * @param amount      valor contestado; se nulo, usa o valor do pagamento
     * @param externalId  id da disputa no PSP (opcional); se nulo, gera um interno
     */
    @Transactional
    public Dispute openDispute(UUID paymentId, String reasonCode, BigDecimal amount, String externalId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new BusinessException("Apenas pagamentos APROVADOS podem ser contestados (atual: "
                + payment.getStatus() + ")");
        }

        BigDecimal disputeAmount = amount != null ? amount : payment.getAmount();
        String reason = (reasonCode != null && !reasonCode.isBlank()) ? reasonCode : "FRAUD";
        String extId = (externalId != null && !externalId.isBlank())
            ? externalId
            : "DSP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Merchant merchant = payment.getMerchant();
        Dispute dispute = new Dispute(merchant, payment, extId, reason, disputeAmount);
        dispute.setReasonDescription("Chargeback aberto pelo emissor/adquirente");
        dispute = disputeRepository.save(dispute);

        log.info("Disputa {} aberta sobre pagamento {} — motivo {}, valor {}",
            dispute.getExternalId(), payment.getReferenceId(), reason, disputeAmount);

        webhookService.notifyMerchant(merchant, "DISPUTE_CREATED", disputePayload(dispute, "DISPUTE_CREATED"));
        webhookService.notifyOrigin(merchant, "DISPUTE_CREATED", disputePayload(dispute, "DISPUTE_CREATED"));
        return dispute;
    }

    private Map<String, Object> disputePayload(Dispute dispute, String eventType) {
        Map<String, Object> body = new HashMap<>();
        body.put("event", eventType);
        body.put("dispute_id", dispute.getId().toString());
        body.put("external_id", dispute.getExternalId());
        body.put("status", dispute.getStatus().name());
        body.put("reason", dispute.getReasonCode());
        body.put("amount", dispute.getDisputeAmount());
        body.put("payment_id", dispute.getPayment().getId().toString());
        body.put("payment_reference_id", dispute.getPayment().getReferenceId());
        body.put("merchant_id", dispute.getMerchant().getId().toString());
        return body;
    }
}
