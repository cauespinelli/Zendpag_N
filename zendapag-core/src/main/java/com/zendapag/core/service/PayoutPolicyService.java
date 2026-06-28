package com.zendapag.core.service;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.PayoutRule;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.PayoutScope;
import com.zendapag.core.repository.PayoutRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Resolve a regra efetiva de liquidação/saque automático para um (merchant, método),
 * em cascata: override do MERCHANT -> regra GLOBAL -> default de código.
 *
 * As regras GLOBAL são semeadas na subida (idempotente) para que o painel de
 * configuração tenha linhas editáveis. O default de código é a rede de segurança
 * caso uma linha não exista.
 *
 * Defaults de fábrica:
 *   PIX          -> sem retenção (D+0), saque automático LIGADO
 *   CREDIT_CARD  -> retenção D+30, saque automático desligado
 *   DEBIT_CARD   -> retenção D+1,  saque automático desligado
 *   BANK_SLIP    -> retenção D+2,  saque automático desligado
 *   BANK_TRANSFER-> retenção D+1,  saque automático desligado
 *   WALLET       -> sem retenção (D+0), saque automático desligado
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutPolicyService {

    /** Regra efetiva já resolvida, com a origem (de onde veio) para exibir no painel. */
    public record EffectiveRule(
        PaymentMethodType method,
        boolean retentionEnabled,
        int holdingDays,
        boolean autoPayoutEnabled,
        PayoutScope source // GLOBAL ou MERCHANT (override) — null vira default de código
    ) {}

    private static final Map<PaymentMethodType, EffectiveRule> DEFAULTS = buildDefaults();

    private static Map<PaymentMethodType, EffectiveRule> buildDefaults() {
        Map<PaymentMethodType, EffectiveRule> m = new EnumMap<>(PaymentMethodType.class);
        m.put(PaymentMethodType.PIX,           new EffectiveRule(PaymentMethodType.PIX,           false, 0,  true,  PayoutScope.GLOBAL));
        m.put(PaymentMethodType.CREDIT_CARD,   new EffectiveRule(PaymentMethodType.CREDIT_CARD,   true,  30, false, PayoutScope.GLOBAL));
        m.put(PaymentMethodType.DEBIT_CARD,    new EffectiveRule(PaymentMethodType.DEBIT_CARD,    true,  1,  false, PayoutScope.GLOBAL));
        m.put(PaymentMethodType.BANK_SLIP,     new EffectiveRule(PaymentMethodType.BANK_SLIP,     true,  2,  false, PayoutScope.GLOBAL));
        m.put(PaymentMethodType.BANK_TRANSFER, new EffectiveRule(PaymentMethodType.BANK_TRANSFER, true,  1,  false, PayoutScope.GLOBAL));
        m.put(PaymentMethodType.WALLET,        new EffectiveRule(PaymentMethodType.WALLET,        false, 0,  false, PayoutScope.GLOBAL));
        return m;
    }

    private final PayoutRuleRepository payoutRuleRepository;

    /** Semeia as regras GLOBAL ausentes na subida da aplicação (idempotente). */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedGlobalDefaults() {
        for (EffectiveRule def : DEFAULTS.values()) {
            boolean exists = payoutRuleRepository
                .findByScopeAndMethodAndMerchantIdIsNull(PayoutScope.GLOBAL, def.method())
                .isPresent();
            if (!exists) {
                payoutRuleRepository.save(new PayoutRule(
                    PayoutScope.GLOBAL, null, def.method(),
                    def.retentionEnabled(), def.holdingDays(), def.autoPayoutEnabled()));
                log.info("[PayoutPolicy] regra GLOBAL semeada: {} (retencao={}, D+{}, autoPayout={})",
                    def.method(), def.retentionEnabled(), def.holdingDays(), def.autoPayoutEnabled());
            }
        }
    }

    /**
     * Regra efetiva para um pagamento: tenta override do merchant, cai para a
     * global, e por fim para o default de código.
     */
    @Transactional(readOnly = true)
    public EffectiveRule resolve(Merchant merchant, PaymentMethodType method) {
        PaymentMethodType m = method != null ? method : PaymentMethodType.PIX;

        if (merchant != null && merchant.getId() != null) {
            EffectiveRule override = payoutRuleRepository
                .findByScopeAndMerchantIdAndMethod(PayoutScope.MERCHANT, merchant.getId(), m)
                .map(r -> toEffective(r, PayoutScope.MERCHANT))
                .orElse(null);
            if (override != null) {
                return override;
            }
        }

        return payoutRuleRepository
            .findByScopeAndMethodAndMerchantIdIsNull(PayoutScope.GLOBAL, m)
            .map(r -> toEffective(r, PayoutScope.GLOBAL))
            .orElseGet(() -> DEFAULTS.getOrDefault(m, DEFAULTS.get(PaymentMethodType.PIX)));
    }

    private EffectiveRule toEffective(PayoutRule r, PayoutScope source) {
        return new EffectiveRule(r.getMethod(), r.isRetentionEnabled(), r.getHoldingDays(),
            r.isAutoPayoutEnabled(), source);
    }

    /** Override por merchant: cria ou atualiza uma regra MERCHANT. */
    @Transactional
    public PayoutRule upsertMerchantRule(UUID merchantId, PaymentMethodType method,
                                         boolean retentionEnabled, int holdingDays, boolean autoPayoutEnabled) {
        PayoutRule rule = payoutRuleRepository
            .findByScopeAndMerchantIdAndMethod(PayoutScope.MERCHANT, merchantId, method)
            .orElseGet(() -> new PayoutRule(PayoutScope.MERCHANT, merchantId, method, retentionEnabled, holdingDays, autoPayoutEnabled));
        rule.setRetentionEnabled(retentionEnabled);
        rule.setHoldingDays(holdingDays);
        rule.setAutoPayoutEnabled(autoPayoutEnabled);
        return payoutRuleRepository.save(rule);
    }

    /** Atualiza (ou cria) uma regra GLOBAL de um método. */
    @Transactional
    public PayoutRule upsertGlobalRule(PaymentMethodType method,
                                       boolean retentionEnabled, int holdingDays, boolean autoPayoutEnabled) {
        PayoutRule rule = payoutRuleRepository
            .findByScopeAndMethodAndMerchantIdIsNull(PayoutScope.GLOBAL, method)
            .orElseGet(() -> new PayoutRule(PayoutScope.GLOBAL, null, method, retentionEnabled, holdingDays, autoPayoutEnabled));
        rule.setRetentionEnabled(retentionEnabled);
        rule.setHoldingDays(holdingDays);
        rule.setAutoPayoutEnabled(autoPayoutEnabled);
        return payoutRuleRepository.save(rule);
    }
}
