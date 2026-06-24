package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MOTOR FINANCEIRO (sandbox) — orquestra a aprovação de um pagamento:
 * calcula a taxa (MDR) pelo feeRate do estabelecimento, atualiza o pagamento,
 * delega o crédito de saldo + lançamentos ao LedgerService e enfileira o
 * webhook do evento.
 *
 * Como ainda não há PSP, a aprovação é disparada manualmente (endpoint admin),
 * simulando a confirmação que viria do provedor.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEngineService {

    /** Taxa padrão quando o estabelecimento não tem feeRate configurado (1,99%). */
    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.0199");
    /** Taxa mínima por transação. */
    private static final BigDecimal MIN_FEE = new BigDecimal("0.50");

    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;
    private final WebhookService webhookService;

    @Transactional
    public Payment approvePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Apenas pagamentos PENDING podem ser aprovados (atual: "
                + payment.getStatus() + ")");
        }

        Merchant merchant = payment.getMerchant();
        BigDecimal gross = payment.getAmount();

        // 1) Taxa (MDR) pelo feeRate do estabelecimento, com mínimo
        BigDecimal feeRate = merchant.getFeeRate() != null ? merchant.getFeeRate() : DEFAULT_FEE_RATE;
        BigDecimal fee = gross.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        if (fee.compareTo(MIN_FEE) < 0) {
            fee = MIN_FEE;
        }
        BigDecimal net = gross.subtract(fee);

        // 2) Atualiza o pagamento
        payment.setFeeAmount(fee);
        payment.setNetAmount(net);
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        log.info("Pagamento {} aprovado — bruto {}, taxa {} ({}%), líquido {}",
            payment.getReferenceId(), gross, fee, feeRate.multiply(new BigDecimal("100")), net);

        // 3) Razão: credita líquido no saldo do merchant + registra taxa como receita
        ledgerService.settleApprovedPayment(payment, gross, fee, net);

        // 4) Webhook do evento (enfileira; entrega HTTP real vem no item 3)
        enqueueWebhook(merchant, payment, "PAYMENT_COMPLETED");

        return payment;
    }

    private void enqueueWebhook(Merchant merchant, Payment payment, String eventType) {
        try {
            if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().isEmpty()) {
                log.info("Estabelecimento {} sem webhookUrl — webhook não enfileirado.", merchant.getId());
                return;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("event", eventType);
            body.put("payment_id", payment.getId().toString());
            body.put("reference_id", payment.getReferenceId());
            body.put("status", payment.getStatus().name());
            body.put("amount", payment.getAmount());
            body.put("fee", payment.getFeeAmount());
            body.put("net", payment.getNetAmount());
            body.put("merchant_id", merchant.getId().toString());
            webhookService.sendMerchantWebhook(merchant, eventType, body);
            log.info("Webhook {} enfileirado para o estabelecimento {}.", eventType, merchant.getId());
        } catch (Exception e) {
            // Webhook nunca deve impedir a aprovação do pagamento
            log.warn("Falha ao enfileirar webhook {}: {}", eventType, e.getMessage());
        }
    }
}
