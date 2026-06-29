package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.PaymentMethod;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PaymentMethodRepository;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.service.boleto.BoletoProvider;
import com.zendapag.core.service.boleto.BoletoProvider.BoletoIssueRequest;
import com.zendapag.core.service.boleto.BoletoProvider.BoletoIssueResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cobrança com BOLETO (provider-ready).
 *
 * Boleto é ASSÍNCRONO: aqui apenas EMITIMOS o boleto (código de barras, linha
 * digitável, vencimento, url) e o pagamento fica PENDING. A confirmação chega
 * depois, quando o cliente paga e o banco/PSP envia o webhook de ENTRADA
 * (boleto.paid -> PaymentEngineService.approvePayment, com tarifa fixa + saldo
 * pendente D+2 da Fase 1).
 */
@Service
@Slf4j
public class BoletoPaymentService {

    private final Map<String, BoletoProvider> providers;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Value("${zendapag.boleto.default:sandbox}")
    private String defaultProvider;
    /** Prazo de vencimento padrão do boleto, em dias. */
    @Value("${zendapag.boleto.due-days:3}")
    private int defaultDueDays;

    public BoletoPaymentService(List<BoletoProvider> providerBeans,
                                PaymentRepository paymentRepository,
                                MerchantRepository merchantRepository,
                                PaymentMethodRepository paymentMethodRepository) {
        this.providers = providerBeans.stream()
            .collect(Collectors.toMap(BoletoProvider::providerKey, p -> p));
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public record BoletoChargeRequest(
        String referenceId, BigDecimal amount, Integer dueInDays,
        String customerName, String customerEmail, String customerDocument,
        String description, String notificationUrl
    ) {}

    @Transactional
    public Payment createBoletoPayment(UUID merchantId, BoletoChargeRequest req) {
        if (req.amount() == null || req.amount().signum() <= 0) {
            throw new BusinessException("Valor inválido");
        }

        // Idempotência: mesmo referenceId não emite dois boletos (busca sem cache)
        if (req.referenceId() != null) {
            Payment existing = paymentRepository.findByReferenceIdUncached(req.referenceId()).orElse(null);
            if (existing != null) {
                log.info("[Boleto] referenceId {} já existe — idempotente, devolvendo o pagamento", req.referenceId());
                return existing;
            }
        }

        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));

        PaymentMethod pm = new PaymentMethod(merchant, PaymentMethodType.BANK_SLIP);
        pm = paymentMethodRepository.save(pm);

        Payment payment = new Payment(
            req.referenceId() != null ? req.referenceId() : generateReferenceId(),
            merchant, req.amount());
        payment.setCurrency("BRL");
        payment.setPaymentMethod(pm);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCustomerName(req.customerName());
        payment.setCustomerEmail(req.customerEmail());
        payment.setCustomerDocument(req.customerDocument());
        payment.setDescription(req.description());
        payment.setNotificationUrl(req.notificationUrl());
        payment.setGateway(defaultProvider);

        int dueInDays = req.dueInDays() != null && req.dueInDays() > 0 ? req.dueInDays() : defaultDueDays;
        BoletoProvider provider = resolveProvider();
        BoletoIssueResult boleto = provider.issue(new BoletoIssueRequest(
            payment.getReferenceId(), req.amount(), dueInDays, req.customerName(), req.customerDocument()));

        payment.setBoletoBarcode(boleto.barcode());
        payment.setBoletoDigitableLine(boleto.digitableLine());
        payment.setBoletoDueDate(boleto.dueDate());
        payment.setBoletoUrl(boleto.url());
        // Vencimento do pagamento alinhado ao do boleto (fim do dia do vencimento)
        payment.setExpiresAt(boleto.dueDate().plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant());

        Payment saved = paymentRepository.save(payment);
        log.info("[Boleto] emitido {} — valor {}, venc {} (aguardando pagamento)",
            saved.getReferenceId(), req.amount(), boleto.dueDate());
        return saved;
    }

    private BoletoProvider resolveProvider() {
        BoletoProvider p = providers.get(defaultProvider);
        if (p == null) {
            throw new BusinessException("Emissor de boleto não configurado: " + defaultProvider);
        }
        return p;
    }

    private String generateReferenceId() {
        return "BOL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
