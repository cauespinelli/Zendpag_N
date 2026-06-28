package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.PayoutScope;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Regra de liquidação e saque automático, por método de pagamento.
 *
 * Duas camadas resolvidas em cascata (ver PayoutPolicyService):
 *  - GLOBAL   (merchantId nulo): padrão da plataforma, uma linha por método.
 *  - MERCHANT (merchantId preenchido): override de um estabelecimento.
 *
 * Campos:
 *  - retentionEnabled: se o líquido entra como PENDENTE (true) ou já DISPONÍVEL (false).
 *  - holdingDays: D+N de retenção quando retentionEnabled (0 = D+0, libera na hora).
 *  - autoPayoutEnabled: ao haver saldo disponível, dispara saque automático à chave do merchant.
 */
@Entity
@Table(name = "payout_rules", uniqueConstraints = {
    @UniqueConstraint(name = "uk_payout_rule_scope_merchant_method",
        columnNames = {"scope", "merchant_id", "method"})
}, indexes = {
    @Index(name = "idx_payout_rule_scope", columnList = "scope"),
    @Index(name = "idx_payout_rule_merchant", columnList = "merchant_id")
})
public class PayoutRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PayoutScope scope;

    /** Nulo para regras GLOBAL; id do estabelecimento para overrides MERCHANT. */
    @Column(name = "merchant_id")
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethodType method;

    @Column(name = "retention_enabled", nullable = false)
    private boolean retentionEnabled;

    @Column(name = "holding_days", nullable = false)
    private int holdingDays;

    @Column(name = "auto_payout_enabled", nullable = false)
    private boolean autoPayoutEnabled;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public PayoutRule() {
    }

    public PayoutRule(PayoutScope scope, UUID merchantId, PaymentMethodType method,
                      boolean retentionEnabled, int holdingDays, boolean autoPayoutEnabled) {
        this.scope = scope;
        this.merchantId = merchantId;
        this.method = method;
        this.retentionEnabled = retentionEnabled;
        this.holdingDays = holdingDays;
        this.autoPayoutEnabled = autoPayoutEnabled;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PayoutScope getScope() { return scope; }
    public void setScope(PayoutScope scope) { this.scope = scope; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public PaymentMethodType getMethod() { return method; }
    public void setMethod(PaymentMethodType method) { this.method = method; }

    public boolean isRetentionEnabled() { return retentionEnabled; }
    public void setRetentionEnabled(boolean retentionEnabled) { this.retentionEnabled = retentionEnabled; }

    public int getHoldingDays() { return holdingDays; }
    public void setHoldingDays(int holdingDays) { this.holdingDays = holdingDays; }

    public boolean isAutoPayoutEnabled() { return autoPayoutEnabled; }
    public void setAutoPayoutEnabled(boolean autoPayoutEnabled) { this.autoPayoutEnabled = autoPayoutEnabled; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
