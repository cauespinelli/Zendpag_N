package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.InboundWebhook;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.entity.enums.InboundEventType;
import com.zendapag.core.entity.enums.InboundWebhookStatus;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.entity.enums.WithdrawalStatus;
import com.zendapag.core.repository.InboundWebhookRepository;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.repository.PixWithdrawalRepository;
import com.zendapag.core.service.inbound.InboundWebhookProvider;
import com.zendapag.core.service.inbound.NormalizedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Núcleo dos webhooks de ENTRADA (PSP -> nós) — o ponto onde o dinheiro é
 * confirmado. Fluxo defensivo: NADA toca o estado antes de validar a assinatura.
 *
 *   1) resolve o adapter do provider (desconhecido -> erro)
 *   2) VERIFICA a assinatura (HMAC). Inválida -> registra INVALID_SIGNATURE e rejeita
 *   3) faz o parse (só agora confia no conteúdo)
 *   4) IDEMPOTÊNCIA: evento já PROCESSED -> DUPLICATE (sem reprocessar)
 *   5) despacha para o motor (aprovar/recusar/concluir saque) + webhook de saída
 *   6) marca PROCESSED / FAILED / IGNORED
 *
 * Defesa em profundidade: mesmo que um replay passe pela idempotência, o motor só
 * aprova pagamento PENDING — nunca credita duas vezes.
 *
 * Não anotado @Transactional no método público de propósito: cada passo persiste
 * em sua própria transação, para que uma falha no motor não apague o registro de
 * auditoria do webhook recebido.
 */
@Service
@Slf4j
public class InboundWebhookService {

    private final Map<String, InboundWebhookProvider> providers;
    private final InboundWebhookRepository inboundWebhookRepository;
    private final PaymentRepository paymentRepository;
    private final PixWithdrawalRepository withdrawalRepository;
    private final PaymentEngineService paymentEngineService;
    private final PixWithdrawalService pixWithdrawalService;
    private final Environment environment;

    public InboundWebhookService(List<InboundWebhookProvider> providerBeans,
                                 InboundWebhookRepository inboundWebhookRepository,
                                 PaymentRepository paymentRepository,
                                 PixWithdrawalRepository withdrawalRepository,
                                 PaymentEngineService paymentEngineService,
                                 PixWithdrawalService pixWithdrawalService,
                                 Environment environment) {
        this.providers = providerBeans.stream()
            .collect(Collectors.toMap(InboundWebhookProvider::providerKey, p -> p));
        this.inboundWebhookRepository = inboundWebhookRepository;
        this.paymentRepository = paymentRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.paymentEngineService = paymentEngineService;
        this.pixWithdrawalService = pixWithdrawalService;
        this.environment = environment;
    }

    /** Resultado para o controller mapear o HTTP. */
    public record ReceiveResult(InboundWebhookStatus status, String message, Long id) {}

    public ReceiveResult receive(String provider, String rawBody, Map<String, String> headers) {
        InboundWebhookProvider adapter = providers.get(provider);
        if (adapter == null) {
            throw new ResourceNotFoundException("Provider de webhook desconhecido: " + provider);
        }

        // 1) Assinatura — barreira de segurança. Sem segredo configurado também rejeita.
        String secret = secretFor(provider);
        boolean valid = secret != null && !secret.isBlank()
            && adapter.verifySignature(rawBody, lower(headers), secret);
        if (!valid) {
            InboundWebhook rec = new InboundWebhook(provider, null, truncate(rawBody));
            rec.setSignatureValid(false);
            rec.setStatus(InboundWebhookStatus.INVALID_SIGNATURE);
            rec.setErrorMessage(secret == null || secret.isBlank()
                ? "Sem segredo configurado para o provider" : "Assinatura ausente ou inválida");
            inboundWebhookRepository.save(rec);
            log.warn("[Inbound] {} REJEITADO: {}", provider, rec.getErrorMessage());
            return new ReceiveResult(InboundWebhookStatus.INVALID_SIGNATURE, "assinatura inválida", rec.getId());
        }

        // 2) Parse — só agora confiamos no conteúdo
        NormalizedEvent event;
        try {
            event = adapter.parse(rawBody);
        } catch (Exception e) {
            InboundWebhook rec = new InboundWebhook(provider, null, truncate(rawBody));
            rec.setSignatureValid(true);
            rec.setStatus(InboundWebhookStatus.FAILED);
            rec.setErrorMessage("Falha no parse: " + e.getMessage());
            inboundWebhookRepository.save(rec);
            return new ReceiveResult(InboundWebhookStatus.FAILED, "payload inválido", rec.getId());
        }

        // 3) Idempotência: evento já processado -> não repete o efeito
        InboundWebhook existing = event.eventId() != null
            ? inboundWebhookRepository.findByProviderAndEventId(provider, event.eventId()).orElse(null)
            : null;
        if (existing != null && existing.getStatus() == InboundWebhookStatus.PROCESSED) {
            log.info("[Inbound] {} evento {} é DUPLICATE (já processado) — ignorado", provider, event.eventId());
            return new ReceiveResult(InboundWebhookStatus.DUPLICATE, "evento já processado", existing.getId());
        }

        InboundWebhook rec = existing != null ? existing
            : new InboundWebhook(provider, event.eventId(), truncate(rawBody));
        rec.setSignatureValid(true);
        rec.setEventType(event.type());
        rec.setReferenceId(event.referenceId());
        rec.setStatus(InboundWebhookStatus.RECEIVED);
        rec.setPayload(truncate(rawBody));
        rec.setErrorMessage(null);
        rec.incrementAttempts();
        inboundWebhookRepository.save(rec);

        return dispatch(rec, event);
    }

    /** Reprocessa um webhook recebido (admin) — repete o parse do payload guardado. */
    public ReceiveResult reprocess(Long id) {
        InboundWebhook rec = inboundWebhookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("InboundWebhook", "id", id));
        if (rec.getStatus() == InboundWebhookStatus.PROCESSED) {
            return new ReceiveResult(InboundWebhookStatus.DUPLICATE, "já estava processado", rec.getId());
        }
        InboundWebhookProvider adapter = providers.get(rec.getProvider());
        if (adapter == null) {
            throw new ResourceNotFoundException("Provider desconhecido: " + rec.getProvider());
        }
        NormalizedEvent event = adapter.parse(rec.getPayload());
        rec.setEventType(event.type());
        rec.setReferenceId(event.referenceId());
        rec.incrementAttempts();
        inboundWebhookRepository.save(rec);
        return dispatch(rec, event);
    }

    private ReceiveResult dispatch(InboundWebhook rec, NormalizedEvent event) {
        try {
            switch (event.type()) {
                case PAYMENT_CONFIRMED -> {
                    Payment p = requirePayment(event.referenceId());
                    if (p.getStatus() != PaymentStatus.PENDING) {
                        // Replay/estado já avançado — idempotente, sem novo crédito
                        return finish(rec, InboundWebhookStatus.DUPLICATE,
                            "pagamento já estava " + p.getStatus(), "pagamento já estava " + p.getStatus());
                    }
                    paymentEngineService.approvePayment(p.getId());
                    log.info("[Inbound] pagamento {} APROVADO via webhook do PSP", p.getReferenceId());
                    return finish(rec, InboundWebhookStatus.PROCESSED, null, "pagamento aprovado");
                }
                case PAYMENT_FAILED -> {
                    Payment p = requirePayment(event.referenceId());
                    if (p.getStatus() != PaymentStatus.PENDING) {
                        return finish(rec, InboundWebhookStatus.DUPLICATE,
                            "pagamento já estava " + p.getStatus(), "pagamento já estava " + p.getStatus());
                    }
                    paymentEngineService.rejectPayment(p.getId(), "Recusado pelo PSP (webhook)");
                    return finish(rec, InboundWebhookStatus.PROCESSED, null, "pagamento recusado");
                }
                case WITHDRAWAL_COMPLETED -> {
                    PixWithdrawal w = withdrawalRepository.findByReferenceId(event.referenceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Saque não encontrado: " + event.referenceId()));
                    if (w.getStatus() == WithdrawalStatus.COMPLETED) {
                        return finish(rec, InboundWebhookStatus.DUPLICATE, "saque já concluído", "saque já concluído");
                    }
                    // Conclui pelo serviço: PROCESSING -> COMPLETED + dispara WITHDRAWAL_COMPLETED
                    pixWithdrawalService.completeWithdrawalFromPsp(event.referenceId(),
                        "PSP-" + (event.eventId() != null ? event.eventId() : "confirm"));
                    return finish(rec, InboundWebhookStatus.PROCESSED, null, "saque concluído");
                }
                default -> {
                    return finish(rec, InboundWebhookStatus.IGNORED,
                        "tipo não mapeado: " + event.rawType(), "evento ignorado");
                }
            }
        } catch (BusinessException | ResourceNotFoundException e) {
            log.warn("[Inbound] falha ao processar evento {} ({}): {}", event.eventId(), event.type(), e.getMessage());
            return finish(rec, InboundWebhookStatus.FAILED, e.getMessage(), e.getMessage());
        } catch (Exception e) {
            log.error("[Inbound] erro inesperado processando evento {}: {}", event.eventId(), e.getMessage(), e);
            return finish(rec, InboundWebhookStatus.FAILED, e.getMessage(), "erro interno");
        }
    }

    private Payment requirePayment(String referenceId) {
        if (referenceId == null) {
            throw new BusinessException("Evento sem referenceId do pagamento");
        }
        return paymentRepository.findByReferenceId(referenceId)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado: " + referenceId));
    }

    private ReceiveResult finish(InboundWebhook rec, InboundWebhookStatus status, String error, String message) {
        rec.setStatus(status);
        rec.setErrorMessage(error);
        rec.setProcessedAt(Instant.now());
        inboundWebhookRepository.save(rec);
        return new ReceiveResult(status, message, rec.getId());
    }

    /** Segredo do PSP por provider, de config: zendapag.psp.<provider>.secret. */
    private String secretFor(String provider) {
        return environment.getProperty("zendapag.psp." + provider + ".secret",
            // default só para o sandbox de DEV
            "sandbox".equals(provider) ? "whsec_psp_sandbox_dev" : null);
    }

    private Map<String, String> lower(Map<String, String> headers) {
        return headers.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue, (a, b) -> a));
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 8000 ? s.substring(0, 8000) : s;
    }
}
