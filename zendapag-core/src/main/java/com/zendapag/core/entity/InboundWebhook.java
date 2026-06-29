package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.InboundEventType;
import com.zendapag.core.entity.enums.InboundWebhookStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Registro de auditoria de cada webhook RECEBIDO do PSP (entrada).
 *
 * A unicidade de (provider, event_id) é a chave de IDEMPOTÊNCIA: o mesmo evento
 * reentregue pelo PSP não pode ser processado duas vezes. Guardamos o payload
 * cru para auditoria e reprocessamento manual.
 */
@Entity
@Table(name = "inbound_webhooks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_inbound_provider_event", columnNames = {"provider", "event_id"})
}, indexes = {
    @Index(name = "idx_inbound_provider", columnList = "provider"),
    @Index(name = "idx_inbound_status", columnList = "status"),
    @Index(name = "idx_inbound_reference", columnList = "reference_id")
})
public class InboundWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 40)
    private String provider;

    /** ID único do evento no PSP. Nulo só quando a assinatura é inválida (não confiável). */
    @Column(name = "event_id", length = 120)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 30)
    private InboundEventType eventType;

    /** referenceId do nosso pagamento/saque que o evento aponta. */
    @Column(name = "reference_id", length = 120)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InboundWebhookStatus status = InboundWebhookStatus.RECEIVED;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid = false;

    /** Payload cru recebido (auditoria / reprocessamento). */
    @Column(name = "payload", length = 8000)
    private String payload;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public InboundWebhook() {
    }

    public InboundWebhook(String provider, String eventId, String payload) {
        this.provider = provider;
        this.eventId = eventId;
        this.payload = payload;
    }

    public Long getId() { return id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public InboundEventType getEventType() { return eventType; }
    public void setEventType(InboundEventType eventType) { this.eventType = eventType; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public InboundWebhookStatus getStatus() { return status; }
    public void setStatus(InboundWebhookStatus status) { this.status = status; }

    public boolean isSignatureValid() { return signatureValid; }
    public void setSignatureValid(boolean signatureValid) { this.signatureValid = signatureValid; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void incrementAttempts() { this.attempts++; }

    public Instant getReceivedAt() { return receivedAt; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
