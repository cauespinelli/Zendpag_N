package com.zendapag.core.service;

import com.zendapag.core.entity.InboundWebhook;
import com.zendapag.core.entity.enums.InboundEventType;
import com.zendapag.core.entity.enums.InboundWebhookStatus;
import com.zendapag.core.repository.InboundWebhookRepository;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.repository.PixWithdrawalRepository;
import com.zendapag.core.service.inbound.InboundWebhookProvider;
import com.zendapag.core.service.inbound.NormalizedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Núcleo dos webhooks de ENTRADA (segurança do dinheiro): rejeita assinatura
 * inválida sem efeito e é idempotente (mesmo evento não processa duas vezes).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InboundWebhookService — assinatura e idempotência")
class InboundWebhookServiceTest {

    @Mock private InboundWebhookProvider adapter;
    @Mock private InboundWebhookRepository inboundRepo;
    @Mock private PaymentRepository paymentRepo;
    @Mock private PixWithdrawalRepository withdrawalRepo;
    @Mock private PaymentEngineService engine;
    @Mock private PixWithdrawalService pixWithdrawalService;
    @Mock private Environment env;

    private InboundWebhookService service;

    @BeforeEach
    void setUp() {
        when(adapter.providerKey()).thenReturn("sandbox");
        lenient().when(env.getProperty(anyString(), anyString())).thenReturn("whsec_psp_sandbox_dev");
        lenient().when(inboundRepo.save(any(InboundWebhook.class))).thenAnswer(i -> i.getArgument(0));
        service = new InboundWebhookService(List.of(adapter), inboundRepo, paymentRepo,
            withdrawalRepo, engine, pixWithdrawalService, env);
    }

    @Test
    @DisplayName("assinatura inválida → INVALID_SIGNATURE e NÃO processa (motor não chamado)")
    void assinaturaInvalidaRejeita() {
        when(adapter.verifySignature(any(), any(), any())).thenReturn(false);

        var result = service.receive("sandbox", "{\"id\":\"evt1\"}", Map.of());

        assertThat(result.status()).isEqualTo(InboundWebhookStatus.INVALID_SIGNATURE);
        verify(engine, never()).approvePayment(any());
        // registra o evento rejeitado (auditoria)
        verify(inboundRepo).save(any(InboundWebhook.class));
    }

    @Test
    @DisplayName("evento já PROCESSED (mesmo eventId) → DUPLICATE, não reprocessa")
    void idempotenciaDuplicado() {
        when(adapter.verifySignature(any(), any(), any())).thenReturn(true);
        when(adapter.parse(any())).thenReturn(
            new NormalizedEvent("evt1", InboundEventType.PAYMENT_CONFIRMED, "PAY-1", "payment.confirmed"));

        InboundWebhook jaProcessado = new InboundWebhook("sandbox", "evt1", "{}");
        jaProcessado.setStatus(InboundWebhookStatus.PROCESSED);
        when(inboundRepo.findByProviderAndEventId(eq("sandbox"), eq("evt1")))
            .thenReturn(Optional.of(jaProcessado));

        var result = service.receive("sandbox", "{\"id\":\"evt1\"}", Map.of());

        assertThat(result.status()).isEqualTo(InboundWebhookStatus.DUPLICATE);
        verify(engine, never()).approvePayment(any());
    }

    @Test
    @DisplayName("provider desconhecido → ResourceNotFound (não processa)")
    void providerDesconhecido() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.receive("inexistente", "{}", Map.of()))
            .isInstanceOf(com.zendapag.common.exception.ResourceNotFoundException.class);
    }
}
