package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.service.CardPaymentService.CardChargeRequest;
import com.zendapag.core.service.CardPaymentService.CardChargeResult;
import com.zendapag.core.service.BoletoPaymentService.BoletoChargeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pagamentos criados por uma ORIGEM externa (gateway) via API Key. Garante que a
 * origem só cria pagamentos para SEUS próprios estabelecimentos (posse validada
 * pelo `source`), e delega ao motor de cada método.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OriginPaymentService {

    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final CardPaymentService cardPaymentService;
    private final BoletoPaymentService boletoPaymentService;

    /** Resolve o estabelecimento e confirma que pertence à origem autenticada. */
    private Merchant resolveOwnedMerchant(String originCode, String merchantId, String externalId) {
        Merchant merchant = null;
        if (merchantId != null && !merchantId.isBlank()) {
            merchant = merchantRepository.findById(UUID.fromString(merchantId)).orElse(null);
        } else if (externalId != null && !externalId.isBlank()) {
            merchant = merchantRepository.findBySourceExternalIdAndSource(externalId, originCode).orElse(null);
        }
        if (merchant == null) {
            throw new ResourceNotFoundException("Estabelecimento não encontrado (merchantId/externalId)");
        }
        if (!originCode.equals(merchant.getSource())) {
            throw new BusinessException("Estabelecimento não pertence à origem " + originCode);
        }
        return merchant;
    }

    /** Cria uma cobrança PIX para um estabelecimento da origem (PENDING; confirma via webhook do PSP). */
    @Transactional
    public Payment createPix(String originCode, String merchantId, String externalId,
                             String referenceId, BigDecimal amount,
                             String customerName, String customerDocument, String customerEmail) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Valor inválido");
        }
        // Idempotência por referenceId (sem cache)
        if (referenceId != null) {
            Payment existing = paymentRepository.findByReferenceIdUncached(referenceId).orElse(null);
            if (existing != null) {
                return existing;
            }
        }
        Merchant merchant = resolveOwnedMerchant(originCode, merchantId, externalId);

        Payment p = new Payment(referenceId != null ? referenceId : generateRef(), merchant, amount);
        p.setCurrency("BRL");
        p.setStatus(PaymentStatus.PENDING);
        p.setCustomerName(customerName);
        p.setCustomerDocument(customerDocument);
        p.setCustomerEmail(customerEmail);
        // Sandbox: "copia e cola" PIX sintético (o PSP real devolveria o EMV verdadeiro)
        p.setPixQrCode("00020126BR.GOV.BCB.PIX-SANDBOX-" + p.getReferenceId());
        Payment saved = paymentRepository.save(p);
        log.info("[OriginPayment {}] PIX criado {} p/ merchant {} (aguardando pagamento)",
            originCode, saved.getReferenceId(), merchant.getId());
        return saved;
    }

    /** Cria uma cobrança com CARTÃO para um estabelecimento da origem (delega ao motor de cartão). */
    @Transactional
    public CardChargeResult createCard(String originCode, String merchantId, String externalId,
                                       CardChargeRequest req) {
        Merchant merchant = resolveOwnedMerchant(originCode, merchantId, externalId);
        return cardPaymentService.createCardPayment(merchant.getId(), req);
    }

    /** Cria uma cobrança com BOLETO para um estabelecimento da origem (delega ao motor de boleto). */
    @Transactional
    public Payment createBoleto(String originCode, String merchantId, String externalId,
                                BoletoChargeRequest req) {
        Merchant merchant = resolveOwnedMerchant(originCode, merchantId, externalId);
        return boletoPaymentService.createBoletoPayment(merchant.getId(), req);
    }

    private String generateRef() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
