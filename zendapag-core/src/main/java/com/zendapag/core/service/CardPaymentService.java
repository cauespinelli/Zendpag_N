package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.PaymentMethod;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.entity.enums.ThreeDsStatus;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PaymentMethodRepository;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.service.acquirer.CardAcquirerProvider;
import com.zendapag.core.service.acquirer.CardAcquirerProvider.CardAuthorizationRequest;
import com.zendapag.core.service.acquirer.CardAcquirerProvider.CardAuthorizationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cobrança com CARTÃO de crédito (provider-ready).
 *
 * Cria o pagamento, anexa um PaymentMethod tokenizado (CREDIT_CARD — sem PAN/CVV),
 * solicita a autorização à adquirente (sandbox por ora) e despacha:
 *   - 3DS exigido  -> fica PENDING (REQUIRED), aguarda confirmação do desafio
 *   - aprovado     -> PaymentEngineService.approvePayment (taxa de cartão + saldo
 *                     PENDENTE D+30 via Fase 1 + webhook de saída)
 *   - recusado     -> rejectPayment (PAYMENT_FAILED + webhook de saída)
 *
 * Não é @Transactional no nível do método de propósito: o Payment é persistido
 * antes de chamar o motor, para que uma retenção por risco (que lança) não apague
 * o pagamento — ele fica PENDING para revisão.
 */
@Service
@Slf4j
public class CardPaymentService {

    private final Map<String, CardAcquirerProvider> acquirers;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentEngineService paymentEngineService;

    @Value("${zendapag.acquirer.default:sandbox}")
    private String defaultAcquirer;

    public CardPaymentService(List<CardAcquirerProvider> acquirerBeans,
                              PaymentRepository paymentRepository,
                              MerchantRepository merchantRepository,
                              PaymentMethodRepository paymentMethodRepository,
                              PaymentEngineService paymentEngineService) {
        this.acquirers = acquirerBeans.stream()
            .collect(Collectors.toMap(CardAcquirerProvider::providerKey, a -> a));
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentEngineService = paymentEngineService;
    }

    /** Dados mínimos de criação de cobrança com cartão (tokenizado). */
    public record CardChargeRequest(
        String referenceId, BigDecimal amount, int installments,
        String cardToken, String brand, String lastFour, Integer expiryMonth, Integer expiryYear,
        String holderName, String customerName, String customerEmail, String customerDocument,
        String description, String notificationUrl
    ) {}

    /** Resultado: o pagamento e, quando houver, o id do desafio 3DS. */
    public record CardChargeResult(Payment payment, String challengeId) {}

    public CardChargeResult createCardPayment(UUID merchantId, CardChargeRequest req) {
        if (req.amount() == null || req.amount().signum() <= 0) {
            throw new BusinessException("Valor inválido");
        }
        if (req.installments() < 1 || req.installments() > 12) {
            throw new BusinessException("Parcelas devem estar entre 1 e 12");
        }
        if (req.cardToken() == null || req.cardToken().isBlank()) {
            throw new BusinessException("Token do cartão é obrigatório");
        }

        // Idempotência: mesmo referenceId não cria/cobra duas vezes (busca sem cache)
        if (req.referenceId() != null) {
            Payment existing = paymentRepository.findByReferenceIdUncached(req.referenceId()).orElse(null);
            if (existing != null) {
                log.info("[Card] referenceId {} já existe — idempotente, devolvendo o pagamento", req.referenceId());
                return new CardChargeResult(existing, null);
            }
        }

        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));

        // PaymentMethod tokenizado (sem PAN/CVV)
        PaymentMethod pm = new PaymentMethod(merchant, PaymentMethodType.CREDIT_CARD);
        pm.setToken(req.cardToken());
        pm.setBrand(req.brand());
        pm.setLastFour(req.lastFour());
        pm.setExpiryMonth(req.expiryMonth());
        pm.setExpiryYear(req.expiryYear());
        pm.setHolderName(req.holderName());
        pm = paymentMethodRepository.save(pm);

        Payment payment = new Payment(
            req.referenceId() != null ? req.referenceId() : generateReferenceId(),
            merchant, req.amount());
        payment.setCurrency("BRL");
        payment.setPaymentMethod(pm);
        payment.setInstallments(req.installments());
        payment.setThreeDsStatus(ThreeDsStatus.NOT_REQUIRED);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCustomerName(req.customerName());
        payment.setCustomerEmail(req.customerEmail());
        payment.setCustomerDocument(req.customerDocument());
        payment.setDescription(req.description());
        payment.setNotificationUrl(req.notificationUrl());
        payment.setGateway(defaultAcquirer);
        payment = paymentRepository.save(payment);

        // Autorização na adquirente
        CardAcquirerProvider acquirer = resolveAcquirer();
        CardAuthorizationResult auth = acquirer.authorize(new CardAuthorizationRequest(
            payment.getReferenceId(), req.cardToken(), req.brand(), req.lastFour(),
            req.amount(), req.installments()));

        switch (auth.threeDs()) {
            case CHALLENGE_REQUIRED -> {
                payment.setThreeDsStatus(ThreeDsStatus.REQUIRED);
                payment.updateMetadata("three_ds_challenge_id", auth.challengeId());
                paymentRepository.save(payment);
                log.info("[Card] {} aguardando 3DS (challenge {})", payment.getReferenceId(), auth.challengeId());
                return new CardChargeResult(payment, auth.challengeId());
            }
            case NOT_REQUIRED, AUTHENTICATED -> {
                if (auth.approved()) {
                    payment.setThreeDsStatus(ThreeDsStatus.AUTHENTICATED);
                    payment.setAuthorizationCode(auth.authorizationCode());
                    payment.setGatewayTransactionId(auth.nsu());
                    paymentRepository.save(payment);
                    return new CardChargeResult(approveViaEngine(payment), null);
                } else {
                    paymentEngineService.rejectPayment(payment.getId(), auth.declineReason());
                    log.info("[Card] {} RECUSADO: {}", payment.getReferenceId(), auth.declineReason());
                    return new CardChargeResult(reload(payment.getId()), null);
                }
            }
            default -> throw new BusinessException("Desfecho 3DS inesperado");
        }
    }

    /** Confirma o desafio 3DS (mock) e prossegue para a autorização/aprovação. */
    public CardChargeResult confirm3ds(UUID paymentId) {
        Payment payment = reload(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING || payment.getThreeDsStatus() != ThreeDsStatus.REQUIRED) {
            throw new BusinessException("Pagamento não está aguardando 3DS (status " + payment.getStatus()
                + ", 3DS " + payment.getThreeDsStatus() + ")");
        }
        payment.setThreeDsStatus(ThreeDsStatus.AUTHENTICATED);
        payment.setAuthorizationCode("AUTH3DS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        paymentRepository.save(payment);
        log.info("[Card] {} 3DS autenticado — prosseguindo para aprovação", payment.getReferenceId());
        return new CardChargeResult(approveViaEngine(payment), null);
    }

    /** Aprova pelo motor; se o risco reter (lança), mantém PENDING e devolve o estado. */
    private Payment approveViaEngine(Payment payment) {
        try {
            return paymentEngineService.approvePayment(payment.getId());
        } catch (BusinessException e) {
            log.warn("[Card] {} retido na aprovação: {}", payment.getReferenceId(), e.getMessage());
            return reload(payment.getId());
        }
    }

    private CardAcquirerProvider resolveAcquirer() {
        CardAcquirerProvider a = acquirers.get(defaultAcquirer);
        if (a == null) {
            throw new BusinessException("Adquirente não configurada: " + defaultAcquirer);
        }
        return a;
    }

    private Payment reload(UUID id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
    }

    private String generateReferenceId() {
        return "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
