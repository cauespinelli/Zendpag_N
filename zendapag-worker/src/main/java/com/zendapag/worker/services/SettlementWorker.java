package com.zendapag.worker.services;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.TransactionStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.worker.events.SettlementEvent;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.TransactionRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class SettlementWorker {

    private static final Logger logger = LoggerFactory.getLogger(SettlementWorker.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    private final Timer settlementProcessingTimer;

    public SettlementWorker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.settlementProcessingTimer = Timer.builder("settlement.processing.duration")
            .description("Time taken to process settlement events")
            .register(meterRegistry);
    }

    @KafkaListener(
        topics = "settlement-events",
        groupId = "settlement-processor",
        containerFactory = "settlementEventsContainerFactory"
    )
    @Transactional
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void processSettlement(@Payload SettlementEvent event,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start(meterRegistry);
        UUID settlementId = UUID.randomUUID();

        logger.info("Processing settlement event - ID: {}, Merchant: {}, Period: {}",
            settlementId, event.getMerchantId(), event.getPeriod());

        try {
            validateSettlementEvent(event);
            Merchant merchant = getMerchant(event.getMerchantId());

            logger.info("Settlement processed successfully - ID: {}, Merchant: {}",
                settlementId, merchant.getId());

            acknowledgment.acknowledge();

        } catch (SettlementValidationException e) {
            logger.error("Settlement validation failed - ID: {}, Error: {}", settlementId, e.getMessage());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Settlement processing failed - ID: {}, will retry", settlementId, e);
            kafkaTemplate.send("settlement-dead-letter-queue", event);
            acknowledgment.acknowledge();

        } finally {
            sample.stop(settlementProcessingTimer);
        }
    }

    private void validateSettlementEvent(SettlementEvent event) {
        if (event.getMerchantId() == null) {
            throw new SettlementValidationException("Merchant ID is required");
        }
        if (event.getPeriod() == null || event.getPeriod().trim().isEmpty()) {
            throw new SettlementValidationException("Settlement period is required");
        }
        if (event.getSettlementDate() == null) {
            throw new SettlementValidationException("Settlement date is required");
        }
        logger.debug("Settlement event validation passed for merchant: {}", event.getMerchantId());
    }

    private Merchant getMerchant(UUID merchantId) {
        Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);
        if (merchantOpt.isEmpty()) {
            throw new SettlementValidationException("Merchant not found: " + merchantId);
        }
        Merchant merchant = merchantOpt.get();
        if (!merchant.isActive()) {
            throw new SettlementValidationException("Merchant is not active: " + merchantId);
        }
        return merchant;
    }

    public static class SettlementValidationException extends RuntimeException {
        public SettlementValidationException(String message) {
            super(message);
        }
    }

    public static class SettlementProcessingException extends RuntimeException {
        public SettlementProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
