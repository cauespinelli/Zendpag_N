package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.PayoutRule;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.service.MerchantBalanceService;
import com.zendapag.core.service.PayoutPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Painel admin: configuração das regras de liquidação/saque automático e
 * consulta de saldos por método. Tudo sob /api/v1/admin/** (ADMIN).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminPayoutController {

    private final PayoutPolicyService payoutPolicyService;
    private final MerchantBalanceService merchantBalanceService;

    /** Corpo de upsert de regra (global ou override). */
    public record RuleRequest(String method, boolean retentionEnabled, int holdingDays, boolean autoPayoutEnabled) {}

    // ---- Regras GLOBAIS ----

    @GetMapping("/payout-rules")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listGlobal() {
        List<Map<String, Object>> rules = payoutPolicyService.globalRules().stream()
            .sorted((a, b) -> a.getMethod().name().compareTo(b.getMethod().name()))
            .map(this::ruleToMap)
            .toList();
        return ResponseEntity.ok(ApiResponse.success("Regras globais", rules));
    }

    @PutMapping("/payout-rules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upsertGlobal(@RequestBody RuleRequest req) {
        PayoutRule rule = payoutPolicyService.upsertGlobalRule(
            PaymentMethodType.valueOf(req.method()), req.retentionEnabled(), req.holdingDays(), req.autoPayoutEnabled());
        log.info("[AdminPayout] regra GLOBAL {} atualizada: retencao={}, D+{}, autoPayout={}",
            rule.getMethod(), rule.isRetentionEnabled(), rule.getHoldingDays(), rule.isAutoPayoutEnabled());
        return ResponseEntity.ok(ApiResponse.success("Regra global salva", ruleToMap(rule)));
    }

    // ---- Regras POR ESTABELECIMENTO (efetivas + overrides) ----

    @GetMapping("/payout-rules/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listEffective(@PathVariable UUID merchantId) {
        // Marca quais métodos têm override próprio
        var overridden = payoutPolicyService.merchantRules(merchantId).stream()
            .map(PayoutRule::getMethod).toList();

        List<Map<String, Object>> out = new ArrayList<>();
        for (PaymentMethodType method : PaymentMethodType.values()) {
            PayoutPolicyService.EffectiveRule eff = payoutPolicyService.resolve(merchantId, method);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("method", method.name());
            m.put("retentionEnabled", eff.retentionEnabled());
            m.put("holdingDays", eff.holdingDays());
            m.put("autoPayoutEnabled", eff.autoPayoutEnabled());
            m.put("source", eff.source() != null ? eff.source().name() : "GLOBAL");
            m.put("overridden", overridden.contains(method));
            out.add(m);
        }
        return ResponseEntity.ok(ApiResponse.success("Regras efetivas do estabelecimento", out));
    }

    @PutMapping("/payout-rules/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upsertMerchant(
            @PathVariable UUID merchantId, @RequestBody RuleRequest req) {
        PayoutRule rule = payoutPolicyService.upsertMerchantRule(
            merchantId, PaymentMethodType.valueOf(req.method()),
            req.retentionEnabled(), req.holdingDays(), req.autoPayoutEnabled());
        log.info("[AdminPayout] override {} do merchant {} salvo: retencao={}, D+{}, autoPayout={}",
            rule.getMethod(), merchantId, rule.isRetentionEnabled(), rule.getHoldingDays(), rule.isAutoPayoutEnabled());
        return ResponseEntity.ok(ApiResponse.success("Override salvo", ruleToMap(rule)));
    }

    @DeleteMapping("/payout-rules/merchant/{merchantId}/{method}")
    public ResponseEntity<ApiResponse<String>> deleteMerchantOverride(
            @PathVariable UUID merchantId, @PathVariable String method) {
        payoutPolicyService.deleteMerchantRule(merchantId, PaymentMethodType.valueOf(method));
        return ResponseEntity.ok(ApiResponse.success("Override removido (volta a herdar a global)", method));
    }

    // ---- Saldos por método ----

    @GetMapping("/merchants/{merchantId}/balances")
    public ResponseEntity<ApiResponse<MerchantBalanceService.MerchantBalances>> balances(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(ApiResponse.success("Saldos do estabelecimento",
            merchantBalanceService.getBalances(merchantId)));
    }

    private Map<String, Object> ruleToMap(PayoutRule r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("method", r.getMethod().name());
        m.put("retentionEnabled", r.isRetentionEnabled());
        m.put("holdingDays", r.getHoldingDays());
        m.put("autoPayoutEnabled", r.isAutoPayoutEnabled());
        m.put("scope", r.getScope().name());
        return m;
    }
}
