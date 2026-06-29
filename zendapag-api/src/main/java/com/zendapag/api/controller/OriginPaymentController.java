package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.service.BoletoPaymentService.BoletoChargeRequest;
import com.zendapag.core.service.CardPaymentService.CardChargeRequest;
import com.zendapag.core.service.CardPaymentService.CardChargeResult;
import com.zendapag.core.service.OriginPaymentService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pagamentos enviados pela ORIGEM externa (gateway) via API Key (ROLE_ORIGIN).
 * O `source` vem da API Key; a origem só cria pagamentos para SEUS merchants.
 *
 * POST /api/v1/origin/payments/pix | /card | /boleto
 */
@RestController
@RequestMapping("/api/v1/origin/payments")
@PreAuthorize("hasRole('ORIGIN')")
@RequiredArgsConstructor
@Slf4j
public class OriginPaymentController {

    private final OriginPaymentService originPaymentService;

    /** Identificação do estabelecimento: merchantId (UUID da Zend) ou externalId (id no gateway). */
    public static class BasePaymentRequest {
        public String merchantId;
        public String externalId;
        public String referenceId;
        @NotNull public BigDecimal amount;
        public String customerName;
        public String customerDocument;
        public String customerEmail;
    }

    public static class CardPaymentRequest extends BasePaymentRequest {
        public Integer installments = 1;
        public String cardToken;
        public String brand;
        public String lastFour;
        public Integer expiryMonth;
        public Integer expiryYear;
        public String holderName;
    }

    public static class BoletoPaymentRequest extends BasePaymentRequest {
        public Integer dueInDays;
    }

    @PostMapping("/pix")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pix(
            @RequestBody BasePaymentRequest req, Authentication auth) {
        String source = auth.getName();
        Payment p = originPaymentService.createPix(source, req.merchantId, req.externalId,
            req.referenceId, req.amount, req.customerName, req.customerDocument, req.customerEmail);
        Map<String, Object> body = paymentMap(p, source);
        body.put("pix_copia_e_cola", p.getPixQrCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("PIX criado", body));
    }

    @PostMapping("/card")
    public ResponseEntity<ApiResponse<Map<String, Object>>> card(
            @RequestBody CardPaymentRequest req, Authentication auth) {
        String source = auth.getName();
        CardChargeResult result = originPaymentService.createCard(source, req.merchantId, req.externalId,
            new CardChargeRequest(req.referenceId, req.amount, req.installments != null ? req.installments : 1,
                req.cardToken, req.brand, req.lastFour, req.expiryMonth, req.expiryYear, req.holderName,
                req.customerName, req.customerEmail, req.customerDocument, null, null));
        Map<String, Object> body = paymentMap(result.payment(), source);
        body.put("challenge_id", result.challengeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Cartão criado", body));
    }

    @PostMapping("/boleto")
    public ResponseEntity<ApiResponse<Map<String, Object>>> boleto(
            @RequestBody BoletoPaymentRequest req, Authentication auth) {
        String source = auth.getName();
        Payment p = originPaymentService.createBoleto(source, req.merchantId, req.externalId,
            new BoletoChargeRequest(req.referenceId, req.amount, req.dueInDays,
                req.customerName, req.customerEmail, req.customerDocument, null, null));
        Map<String, Object> body = paymentMap(p, source);
        body.put("barcode", p.getBoletoBarcode());
        body.put("digitable_line", p.getBoletoDigitableLine());
        body.put("due_date", p.getBoletoDueDate() != null ? p.getBoletoDueDate().toString() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Boleto criado", body));
    }

    private Map<String, Object> paymentMap(Payment p, String source) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("payment_id", p.getId().toString());
        m.put("reference_id", p.getReferenceId());
        m.put("status", p.getStatus().name());
        m.put("amount", p.getAmount());
        m.put("merchant_id", p.getMerchant().getId().toString());
        m.put("source", source);
        return m;
    }
}
