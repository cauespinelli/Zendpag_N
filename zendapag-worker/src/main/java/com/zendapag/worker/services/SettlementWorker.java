package com.zendapag.worker.services;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.TransactionStatus;
import com.zendapag.core.entity.TransactionType;
import com.zendapag.core.events.SettlementEvent;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.TransactionRepository;
import com.zendapag.core.service.FeesService;
import com.zendapag.integration.banking.BankingClient;

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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Settlement worker for processing merchant settlements
 * Handles automatic settlement calculation, fee deduction, and payout initiation
 */
@Component
public class SettlementWorker {

    private static final Logger logger = LoggerFactory.getLogger(SettlementWorker.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private FeesService feesService;

    @Autowired
    private BankingClient bankingClient;

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

    /**
     * Process settlement events from Kafka
     */
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
                                 @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment acknowledgment) {

        Timer.Sample sample = settlementProcessingTimer.start();
        UUID settlementId = UUID.randomUUID();

        logger.info("Processing settlement event - ID: {}, Merchant: {}, Period: {}",
            settlementId, event.getMerchantId(), event.getPeriod());

        try {
            // Validate event
            validateSettlementEvent(event);

            // Get merchant
            Merchant merchant = getMerchant(event.getMerchantId());

            // Calculate settlement
            SettlementCalculation calculation = calculateSettlement(merchant, event);

            if (calculation.getNetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Settlement amount is zero or negative for merchant {} - skipping payout",
                    merchant.getId());

                // Still create settlement record for audit
                createSettlementRecord(merchant, event, calculation, TransactionStatus.COMPLETED);

                // Update metrics
                meterRegistry.counter("settlement.processed",
                    "merchant", merchant.getId().toString(),
                    "status", "zero_amount"
                ).increment();

                acknowledgment.acknowledge();
                return;
            }

            // Create settlement transaction
            Transaction settlementTransaction = createSettlementTransaction(merchant, event, calculation);

            // Send to banking partner for payout
            initiatePayoutToBankingPartner(merchant, settlementTransaction, calculation);

            // Update metrics
            updateSettlementMetrics(merchant, calculation, "success");

            logger.info("Settlement processed successfully - ID: {}, Merchant: {}, Net Amount: {}",
                settlementId, merchant.getId(), calculation.getNetAmount());

            acknowledgment.acknowledge();

        } catch (SettlementValidationException e) {
            logger.error("Settlement validation failed - ID: {}, Error: {}", settlementId, e.getMessage());

            meterRegistry.counter("settlement.validation_errors",
                "merchant", event.getMerchantId().toString(),
                "error", e.getClass().getSimpleName()
            ).increment();

            // Don't retry validation errors
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Settlement processing failed - ID: {}, will retry", settlementId, e);

            meterRegistry.counter("settlement.processing_errors",
                "merchant", event.getMerchantId().toString(),
                "error", e.getClass().getSimpleName()
            ).increment();

            // Send to DLQ if max retries exceeded
            kafkaTemplate.send("settlement-dead-letter-queue", event);
            acknowledgment.acknowledge();

        } finally {
            sample.stop();
        }
    }

    /**
     * Validate settlement event
     */
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

    /**
     * Get merchant entity
     */
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

    /**
     * Calculate settlement amounts including fees
     */
    private SettlementCalculation calculateSettlement(Merchant merchant, SettlementEvent event) {
        logger.debug("Calculating settlement for merchant: {}, period: {}", merchant.getId(), event.getPeriod());

        // Get transactions for the settlement period
        List<Transaction> transactions = getTransactionsForPeriod(merchant.getId(), event);

        if (transactions.isEmpty()) {
            logger.warn("No transactions found for settlement - Merchant: {}, Period: {}",
                merchant.getId(), event.getPeriod());

            return SettlementCalculation.builder()
                .merchantId(merchant.getId())
                .period(event.getPeriod())
                .transactionCount(0)
                .grossAmount(BigDecimal.ZERO)
                .totalFees(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .build();
        }

        // Calculate gross amount (sum of all completed transactions)
        BigDecimal grossAmount = transactions.stream()
            .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total fees
        BigDecimal totalFees = calculateTotalFees(merchant, transactions);

        // Calculate net amount
        BigDecimal netAmount = grossAmount.subtract(totalFees);

        logger.debug("Settlement calculation completed - Gross: {}, Fees: {}, Net: {}",
            grossAmount, totalFees, netAmount);

        return SettlementCalculation.builder()
            .merchantId(merchant.getId())
            .period(event.getPeriod())
            .transactionCount(transactions.size())
            .grossAmount(grossAmount)
            .totalFees(totalFees)
            .netAmount(netAmount)
            .transactions(transactions)
            .build();
    }

    /**
     * Get transactions for settlement period
     */
    private List<Transaction> getTransactionsForPeriod(UUID merchantId, SettlementEvent event) {
        // Parse period (assuming format: YYYY-MM-DD)
        LocalDate settlementDate = LocalDate.parse(event.getPeriod());
        LocalDateTime startOfDay = settlementDate.atStartOfDay();
        LocalDateTime endOfDay = settlementDate.atTime(23, 59, 59);

        return transactionRepository.findByMerchantIdAndCreatedAtBetween(
            merchantId, startOfDay, endOfDay
        );
    }

    /**
     * Calculate total fees for transactions
     */
    private BigDecimal calculateTotalFees(Merchant merchant, List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
            .map(t -> feesService.calculateTransactionFee(merchant, t))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Create settlement transaction record
     */
    private Transaction createSettlementTransaction(Merchant merchant, SettlementEvent event,
                                                   SettlementCalculation calculation) {

        String description = String.format("Settlement for %s (%d transactions)",
            event.getPeriod(), calculation.getTransactionCount());

        Transaction settlementTransaction = Transaction.builder()
            .merchantId(merchant.getId())
            .type(TransactionType.SETTLEMENT)
            .amount(calculation.getNetAmount())
            .originalAmount(calculation.getGrossAmount())
            .feeAmount(calculation.getTotalFees())
            .description(description)
            .status(TransactionStatus.PENDING)
            .externalId("SETTLEMENT_" + event.getPeriod() + "_" + merchant.getId())
            .metadata(createSettlementMetadata(calculation))
            .build();

        return transactionRepository.save(settlementTransaction);
    }

    /**
     * Create settlement record for audit purposes
     */
    private Transaction createSettlementRecord(Merchant merchant, SettlementEvent event,
                                             SettlementCalculation calculation, TransactionStatus status) {

        String description = String.format("Settlement record for %s (%d transactions)",
            event.getPeriod(), calculation.getTransactionCount());

        Transaction settlementRecord = Transaction.builder()
            .merchantId(merchant.getId())
            .type(TransactionType.SETTLEMENT)
            .amount(calculation.getNetAmount())
            .originalAmount(calculation.getGrossAmount())
            .feeAmount(calculation.getTotalFees())
            .description(description)
            .status(status)
            .externalId("SETTLEMENT_RECORD_" + event.getPeriod() + "_" + merchant.getId())
            .metadata(createSettlementMetadata(calculation))
            .build();

        return transactionRepository.save(settlementRecord);
    }

    /**
     * Create settlement metadata
     */
    private String createSettlementMetadata(SettlementCalculation calculation) {
        return String.format("""
            {
                "settlementPeriod": "%s",
                "transactionCount": %d,
                "grossAmount": "%s",
                "totalFees": "%s",
                "netAmount": "%s",
                "feeRate": "%.4f",
                "processedAt": "%s"
            }
            """,
            calculation.getPeriod(),
            calculation.getTransactionCount(),
            calculation.getGrossAmount().toString(),
            calculation.getTotalFees().toString(),
            calculation.getNetAmount().toString(),
            calculation.getGrossAmount().compareTo(BigDecimal.ZERO) > 0 ?
                calculation.getTotalFees().divide(calculation.getGrossAmount(), 4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO,
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    /**
     * Initiate payout to banking partner
     */
    private void initiatePayoutToBankingPartner(Merchant merchant, Transaction settlementTransaction,
                                              SettlementCalculation calculation) {
        try {
            logger.info("Initiating payout to banking partner - Merchant: {}, Amount: {}",
                merchant.getId(), calculation.getNetAmount());

            // Create payout request
            BankingClient.PayoutRequest payoutRequest = BankingClient.PayoutRequest.builder()
                .merchantId(merchant.getId().toString())
                .amount(calculation.getNetAmount())
                .currency("BRL")
                .description("Settlement payout for " + calculation.getPeriod())
                .externalReference(settlementTransaction.getId().toString())
                .bankAccount(merchant.getBankAccountInfo())
                .build();

            // Send payout request to banking partner
            BankingClient.PayoutResponse payoutResponse = bankingClient.createPayout(payoutRequest);

            // Update settlement transaction with payout details
            settlementTransaction.setExternalId(payoutResponse.getPayoutId());
            settlementTransaction.setStatus(TransactionStatus.PROCESSING);

            // Add payout metadata
            String updatedMetadata = settlementTransaction.getMetadata() + String.format("""
                ,
                "payoutId": "%s",
                "payoutStatus": "%s",
                "payoutInitiatedAt": "%s"
                """,
                payoutResponse.getPayoutId(),
                payoutResponse.getStatus(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

            settlementTransaction.setMetadata(updatedMetadata);
            transactionRepository.save(settlementTransaction);

            logger.info("Payout initiated successfully - Payout ID: {}, Status: {}",
                payoutResponse.getPayoutId(), payoutResponse.getStatus());

        } catch (Exception e) {
            logger.error("Failed to initiate payout for settlement transaction: {}",
                settlementTransaction.getId(), e);

            // Mark settlement as failed
            settlementTransaction.setStatus(TransactionStatus.FAILED);
            settlementTransaction.setErrorMessage(e.getMessage());
            transactionRepository.save(settlementTransaction);

            throw new SettlementProcessingException("Payout initiation failed", e);
        }
    }

    /**
     * Update settlement metrics
     */
    private void updateSettlementMetrics(Merchant merchant, SettlementCalculation calculation, String status) {
        String merchantId = merchant.getId().toString();

        meterRegistry.counter("settlement.processed",
            "merchant", merchantId,
            "status", status
        ).increment();

        meterRegistry.gauge("settlement.amount",
            calculation.getNetAmount().doubleValue());

        meterRegistry.gauge("settlement.transaction_count",
            calculation.getTransactionCount());

        meterRegistry.gauge("settlement.fee_amount",
            calculation.getTotalFees().doubleValue());
    }

    /**
     * Settlement calculation result
     */
    public static class SettlementCalculation {
        private UUID merchantId;
        private String period;
        private int transactionCount;
        private BigDecimal grossAmount;
        private BigDecimal totalFees;
        private BigDecimal netAmount;
        private List<Transaction> transactions;

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public UUID getMerchantId() { return merchantId; }
        public String getPeriod() { return period; }
        public int getTransactionCount() { return transactionCount; }
        public BigDecimal getGrossAmount() { return grossAmount; }
        public BigDecimal getTotalFees() { return totalFees; }
        public BigDecimal getNetAmount() { return netAmount; }
        public List<Transaction> getTransactions() { return transactions; }

        public static class Builder {
            private SettlementCalculation calculation = new SettlementCalculation();

            public Builder merchantId(UUID merchantId) {
                calculation.merchantId = merchantId;
                return this;
            }

            public Builder period(String period) {
                calculation.period = period;
                return this;
            }

            public Builder transactionCount(int count) {
                calculation.transactionCount = count;
                return this;
            }

            public Builder grossAmount(BigDecimal amount) {
                calculation.grossAmount = amount;
                return this;
            }

            public Builder totalFees(BigDecimal fees) {
                calculation.totalFees = fees;
                return this;
            }

            public Builder netAmount(BigDecimal amount) {
                calculation.netAmount = amount;
                return this;
            }

            public Builder transactions(List<Transaction> transactions) {
                calculation.transactions = transactions;
                return this;
            }

            public SettlementCalculation build() {
                return calculation;
            }
        }
    }

    /**
     * Settlement validation exception
     */
    public static class SettlementValidationException extends RuntimeException {
        public SettlementValidationException(String message) {
            super(message);
        }
    }

    /**
     * Settlement processing exception
     */
    public static class SettlementProcessingException extends RuntimeException {
        public SettlementProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}