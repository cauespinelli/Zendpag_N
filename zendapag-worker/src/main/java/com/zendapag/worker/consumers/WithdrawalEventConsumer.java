package com.zendapag.worker.consumers;

import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.service.PixWithdrawalService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka consumer para processamento assíncrono de saques PIX
 */
@Component
@Slf4j
@KafkaListener(
    topics = "withdrawal-events",
    groupId = "withdrawal-processor",
    containerFactory = "kafkaListenerContainerFactory"
)
public class WithdrawalEventConsumer {

    private final PixWithdrawalService withdrawalService;

    // Metrics
    private final Counter eventCounter;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;

    public WithdrawalEventConsumer(PixWithdrawalService withdrawalService,
                                  MeterRegistry meterRegistry) {
        this.withdrawalService = withdrawalService;

        this.eventCounter = Counter.builder("kafka.withdrawal.events.received")
                .tag("topic", "withdrawal-events")
                .description("Total withdrawal events received")
                .register(meterRegistry);

        this.successCounter = Counter.builder("kafka.withdrawal.events.success")
                .tag("topic", "withdrawal-events")
                .description("Successful withdrawal events processed")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("kafka.withdrawal.events.error")
                .tag("topic", "withdrawal-events")
                .description("Failed withdrawal events")
                .register(meterRegistry);

        this.processingTimer = Timer.builder("kafka.withdrawal.events.processing.time")
                .tag("topic", "withdrawal-events")
                .description("Time taken to process withdrawal events")
                .register(meterRegistry);
    }

    /**
     * Processa eventos de saque PIX
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 10000)
    )
    @KafkaListener(topics = "withdrawal-events", groupId = "withdrawal-processor")
    public void handleWithdrawalEvent(@Payload PixWithdrawal withdrawal,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        eventCounter.increment();

        try {
            log.info("Processing withdrawal event: withdrawalId={}, referenceId={}, amount={}, partition={}, offset={}",
                    withdrawal.getId(), withdrawal.getReferenceId(), withdrawal.getAmount(), partition, offset);

            // Processar saque
            processWithdrawal(withdrawal);

            // Commit manual
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            successCounter.increment();
            sample.stop(processingTimer);

            log.info("Withdrawal processed successfully: {}", withdrawal.getReferenceId());

        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error processing withdrawal event: withdrawalId={}, error={}",
                    withdrawal.getId(), e.getMessage(), e);

            // Não fazer acknowledge para reprocessar
            throw e;
        }
    }

    /**
     * Processa saque por ID (formato alternativo)
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 10000)
    )
    @KafkaListener(topics = "withdrawal-processing", groupId = "withdrawal-processor")
    public void handleWithdrawalProcessing(@Payload String withdrawalId,
                                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                          @Header(KafkaHeaders.OFFSET) long offset,
                                          Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        eventCounter.increment();

        try {
            log.info("Processing withdrawal by ID: withdrawalId={}, partition={}, offset={}",
                    withdrawalId, partition, offset);

            UUID id = UUID.fromString(withdrawalId);

            // Processar saque
            withdrawalService.processPixWithdrawal(id.toString());

            // Commit manual
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            successCounter.increment();
            sample.stop(processingTimer);

            log.info("Withdrawal processed successfully: {}", withdrawalId);

        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error processing withdrawal: withdrawalId={}, error={}",
                    withdrawalId, e.getMessage(), e);

            // Não fazer acknowledge para reprocessar
            throw e;
        }
    }

    /**
     * Processa o saque PIX
     */
    private void processWithdrawal(PixWithdrawal withdrawal) {
        try {
            // Chamar o serviço para processar o saque
            withdrawalService.processPixWithdrawal(withdrawal.getId().toString());

            log.debug("Withdrawal processing completed: {}", withdrawal.getReferenceId());

        } catch (Exception e) {
            log.error("Failed to process withdrawal {}: {}", withdrawal.getReferenceId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handler para DLQ (Dead Letter Queue)
     */
    @KafkaListener(topics = "withdrawal-events-dlq", groupId = "withdrawal-dlq-processor")
    public void handleDeadLetterQueue(@Payload String message,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {

        log.error("Message sent to DLQ: message={}, partition={}, offset={}, error={}",
                message, partition, offset, exceptionMessage);

        // Aqui você pode implementar lógica adicional:
        // - Notificar equipe de operações
        // - Armazenar em banco de dados para análise
        // - Enviar alerta
        // - Tentar reprocessamento manual posterior
    }
}
