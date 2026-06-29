package com.zendapag.core.service;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.PayoutRule;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.PayoutScope;
import com.zendapag.core.repository.PayoutRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Regra de liquidação efetiva, resolvida em cascata: override do merchant ->
 * regra global -> default de código.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PayoutPolicyService — resolução em cascata da regra")
class PayoutPolicyServiceTest {

    @Mock private PayoutRuleRepository repo;
    @InjectMocks private PayoutPolicyService service;

    private Merchant merchant(UUID id) {
        Merchant m = new Merchant("L", "00000000000191", "l@l.com");
        m.setId(id);
        return m;
    }

    @Test
    @DisplayName("sem regra no banco → default de código: PIX é D+0 sem retenção e com saque automático")
    void defaultPix() {
        when(repo.findByScopeAndMerchantIdAndMethod(any(), any(), any())).thenReturn(Optional.empty());
        when(repo.findByScopeAndMethodAndMerchantIdIsNull(eq(PayoutScope.GLOBAL), eq(PaymentMethodType.PIX)))
            .thenReturn(Optional.empty());

        var rule = service.resolve(merchant(UUID.randomUUID()), PaymentMethodType.PIX);

        assertThat(rule.retentionEnabled()).isFalse();
        assertThat(rule.holdingDays()).isZero();
        assertThat(rule.autoPayoutEnabled()).isTrue();
    }

    @Test
    @DisplayName("sem regra no banco → default de código: CARTÃO retém D+30 sem saque automático")
    void defaultCartao() {
        when(repo.findByScopeAndMerchantIdAndMethod(any(), any(), any())).thenReturn(Optional.empty());
        when(repo.findByScopeAndMethodAndMerchantIdIsNull(eq(PayoutScope.GLOBAL), eq(PaymentMethodType.CREDIT_CARD)))
            .thenReturn(Optional.empty());

        var rule = service.resolve(merchant(UUID.randomUUID()), PaymentMethodType.CREDIT_CARD);

        assertThat(rule.retentionEnabled()).isTrue();
        assertThat(rule.holdingDays()).isEqualTo(30);
        assertThat(rule.autoPayoutEnabled()).isFalse();
    }

    @Test
    @DisplayName("regra GLOBAL no banco tem precedência sobre o default de código")
    void globalSobreDefault() {
        when(repo.findByScopeAndMerchantIdAndMethod(any(), any(), any())).thenReturn(Optional.empty());
        when(repo.findByScopeAndMethodAndMerchantIdIsNull(eq(PayoutScope.GLOBAL), eq(PaymentMethodType.PIX)))
            .thenReturn(Optional.of(new PayoutRule(PayoutScope.GLOBAL, null, PaymentMethodType.PIX, true, 5, false)));

        var rule = service.resolve(merchant(UUID.randomUUID()), PaymentMethodType.PIX);

        assertThat(rule.retentionEnabled()).isTrue();
        assertThat(rule.holdingDays()).isEqualTo(5);
        assertThat(rule.source()).isEqualTo(PayoutScope.GLOBAL);
    }

    @Test
    @DisplayName("override do MERCHANT tem precedência sobre a global")
    void merchantSobreGlobal() {
        UUID mid = UUID.randomUUID();
        when(repo.findByScopeAndMerchantIdAndMethod(eq(PayoutScope.MERCHANT), eq(mid), eq(PaymentMethodType.PIX)))
            .thenReturn(Optional.of(new PayoutRule(PayoutScope.MERCHANT, mid, PaymentMethodType.PIX, true, 10, true)));

        var rule = service.resolve(merchant(mid), PaymentMethodType.PIX);

        assertThat(rule.retentionEnabled()).isTrue();
        assertThat(rule.holdingDays()).isEqualTo(10);
        assertThat(rule.source()).isEqualTo(PayoutScope.MERCHANT);
    }
}
