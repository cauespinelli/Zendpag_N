package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** Taxa base de cartão de crédito (à vista). Default 3,49%. */
    @Value("${zendapag.fees.card.base-rate:0.0349}")
    private BigDecimal cardBaseRate;
    /** Acréscimo por parcela acima de 1 (juros/risco do parcelado). Default +0,40%/parcela. */
    @Value("${zendapag.fees.card.installment-surcharge:0.0040}")
    private BigDecimal cardInstallmentSurcharge;
    /** Tarifa FIXA por boleto (não percentual). Default R$ 3,49. */
    @Value("${zendapag.fees.boleto.flat:3.49}")
    private BigDecimal boletoFlatFee;

    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;
    private final WebhookService webhookService;
    private final RiskService riskService;

    @Transactional
    public Payment approvePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Apenas pagamentos PENDING podem ser aprovados (atual: "
                + payment.getStatus() + ")");
        }

        // 0) Antifraude: HIGH retém o pagamento (fica PENDING para revisão)
        RiskService.RiskAssessment risk = riskService.assess(payment);
        if (risk.level() == RiskService.RiskLevel.HIGH) {
            log.warn("Pagamento {} RETIDO por risco alto (score {}): {}",
                payment.getReferenceId(), risk.score(), risk.reasons());
            throw new BusinessException("Pagamento retido por risco alto (score " + risk.score()
                + "): " + String.join("; ", risk.reasons()));
        }
        payment.updateMetadata("risk_level", risk.level().name());
        payment.updateMetadata("risk_score", risk.score());
        log.info("Risco do pagamento {}: {} (score {})", payment.getReferenceId(), risk.level(), risk.score());

        Merchant merchant = payment.getMerchant();
        BigDecimal gross = payment.getAmount();

        // 1) Taxa ciente do método:
        //    - BOLETO: tarifa FIXA (não percentual);
        //    - CARTÃO: taxa base + acréscimo por parcela;
        //    - PIX/demais: feeRate do estabelecimento.
        PaymentMethodType method = methodTypeOf(payment);
        BigDecimal fee;
        BigDecimal feeRate;
        if (method == PaymentMethodType.BANK_SLIP) {
            fee = boletoFlatFee.setScale(2, RoundingMode.HALF_UP);
            feeRate = null; // tarifa fixa — sem percentual
        } else {
            feeRate = effectiveFeeRate(payment, merchant);
            fee = gross.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
            if (fee.compareTo(MIN_FEE) < 0) {
                fee = MIN_FEE;
            }
        }
        // A taxa nunca pode exceder o valor bruto: em cobranças menores que a
        // taxa (ex.: boleto de R$1 com tarifa R$3,49), a taxa é limitada ao bruto
        // e o líquido fica em zero — nunca negativo.
        if (fee.compareTo(gross) > 0) {
            fee = gross;
        }
        BigDecimal net = gross.subtract(fee);

        // 2) Atualiza o pagamento
        payment.setFeeAmount(fee);
        payment.setFeeRate(feeRate);
        payment.setNetAmount(net);
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        log.info("Pagamento {} aprovado — bruto {}, taxa {} ({}), líquido {}",
            payment.getReferenceId(), gross, fee,
            feeRate != null ? feeRate.multiply(HUNDRED) + "%" : "tarifa fixa", net);

        // 3) Razão: credita líquido no saldo do merchant + registra taxa como receita
        ledgerService.settleApprovedPayment(payment, gross, fee, net);

        // 4) Webhook do evento
        webhookService.notifyMerchant(merchant, "PAYMENT_COMPLETED", paymentPayload(payment, "PAYMENT_COMPLETED"));

        return payment;
    }

    /** Recusa um pagamento PENDING (sandbox: simula recusa do PSP). Dispara PAYMENT_FAILED. */
    @Transactional
    public Payment rejectPayment(UUID id, String reason) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Apenas pagamentos PENDING podem ser recusados (atual: " + payment.getStatus() + ")");
        }
        payment.setStatus(PaymentStatus.FAILED);
        payment.setCancelledAt(Instant.now());
        payment.setCancellationReason(reason != null ? reason : "Recusado");
        paymentRepository.save(payment);
        log.info("Pagamento {} recusado (FAILED): {}", payment.getReferenceId(), reason);

        webhookService.notifyMerchant(payment.getMerchant(), "PAYMENT_FAILED", paymentPayload(payment, "PAYMENT_FAILED"));
        return payment;
    }

    /** Estorna um pagamento APROVADO: reverte o crédito no razão e dispara PAYMENT_REFUNDED. */
    @Transactional
    public Payment refundPayment(UUID id, String reason) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new BusinessException("Apenas pagamentos APROVADOS podem ser estornados (atual: " + payment.getStatus() + ")");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(Instant.now());
        payment.setRefundReason(reason != null ? reason : "Estorno");
        paymentRepository.save(payment);

        ledgerService.reverseRefund(payment, payment.getNetAmount());
        log.info("Pagamento {} estornado (REFUNDED) — líquido {} revertido", payment.getReferenceId(), payment.getNetAmount());

        webhookService.notifyMerchant(payment.getMerchant(), "PAYMENT_REFUNDED", paymentPayload(payment, "PAYMENT_REFUNDED"));
        return payment;
    }

    /**
     * Taxa efetiva (MDR) por método:
     *  - CARTÃO: taxa base de cartão + acréscimo por parcela acima de 1.
     *  - demais (PIX/boleto): feeRate do estabelecimento (ou default).
     */
    private BigDecimal effectiveFeeRate(Payment payment, Merchant merchant) {
        PaymentMethodType method = methodTypeOf(payment);
        if (method == PaymentMethodType.CREDIT_CARD || method == PaymentMethodType.DEBIT_CARD) {
            int installments = payment.getInstallments() != null ? Math.max(1, payment.getInstallments()) : 1;
            BigDecimal surcharge = cardInstallmentSurcharge.multiply(new BigDecimal(installments - 1));
            return cardBaseRate.add(surcharge);
        }
        return merchant.getFeeRate() != null ? merchant.getFeeRate() : DEFAULT_FEE_RATE;
    }

    /** Tipo do método do pagamento; sem método associado, assume PIX (caso padrão). */
    private PaymentMethodType methodTypeOf(Payment payment) {
        if (payment.getPaymentMethod() != null && payment.getPaymentMethod().getType() != null) {
            return payment.getPaymentMethod().getType();
        }
        return PaymentMethodType.PIX;
    }

    private Map<String, Object> paymentPayload(Payment payment, String eventType) {
        Map<String, Object> body = new HashMap<>();
        body.put("event", eventType);
        body.put("payment_id", payment.getId().toString());
        body.put("reference_id", payment.getReferenceId());
        body.put("status", payment.getStatus().name());
        body.put("amount", payment.getAmount());
        body.put("fee", payment.getFeeAmount());
        body.put("net", payment.getNetAmount());
        body.put("installments", payment.getInstallments());
        body.put("merchant_id", payment.getMerchant().getId().toString());
        return body;
    }
}
