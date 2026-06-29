package com.zendapag.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Origem (tenant) que pode registrar estabelecimentos e transações na Zend.
 * É o "contrato de origem": guarda a API Key (hash) que autentica as chamadas
 * daquela origem e a URL/segredo de webhook para onde a Zend devolve eventos.
 *
 * Ex.: ONE_A_ONE (gateway externo), DIRETO (estabelecimentos próprios da Zend).
 */
@Entity
@Table(name = "origins", uniqueConstraints = {
    @UniqueConstraint(name = "uk_origin_code", columnNames = {"code"})
}, indexes = {
    @Index(name = "idx_origin_api_key_hash", columnList = "api_key_hash")
})
public class Origin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código da origem (usado como `source` em Merchant/Transaction). Ex.: ONE_A_ONE. */
    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", length = 120)
    private String name;

    /** SHA-256 (hex) da API Key. A key em texto puro nunca é armazenada. */
    @Column(name = "api_key_hash", length = 64)
    private String apiKeyHash;

    /** Prefixo exibível da key (ex.: "zk_one_") — só para identificação no painel. */
    @Column(name = "api_key_prefix", length = 20)
    private String apiKeyPrefix;

    /** URL para onde a Zend envia os webhooks de volta desta origem. */
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    /** Segredo HMAC dos webhooks de volta desta origem. */
    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Origin() {
    }

    public Origin(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }

    public String getApiKeyPrefix() { return apiKeyPrefix; }
    public void setApiKeyPrefix(String apiKeyPrefix) { this.apiKeyPrefix = apiKeyPrefix; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
