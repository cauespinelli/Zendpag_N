package com.zendapag.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.repository.TransactionRepository;
import com.zendapag.worker.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessorService {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processTransaction(TransactionEvent event) {
        log.info("Processing transaction: {}", event.getTransactionId());

        // Find existing transaction or create new one
        Transaction transaction = transactionRepository
                .findByTransactionId(event.getTransactionId())
                .orElse(createNewTransaction(event));

        // Update transaction status based on event
        updateTransactionStatus(transaction, event);

        // Simulate PIX processing
        processPixTransaction(transaction, event);

        transactionRepository.save(transaction);
        log.info("Transaction processed successfully: {}", event.getTransactionId());
    }

    @Transactional
    public void processPixWebhook(String webhookPayload) {
        log.info("Processing PIX webhook: {}", webhookPayload);

        try {
            // In a real implementation, you would parse the PIX webhook
            // and update transaction statuses accordingly
            log.info("PIX webhook processed successfully");
        } catch (Exception ex) {
            log.error("Error processing PIX webhook", ex);
            throw ex;
        }
    }

    private Transaction createNewTransaction(TransactionEvent event) {
        return Transaction.builder()
                .transactionId(event.getTransactionId())
                .endToEndId(event.getEndToEndId())
                .type(Transaction.TransactionType.valueOf(event.getType()))
                .amount(event.getAmount())
                .description(event.getDescription())
                .status(Transaction.TransactionStatus.PENDING)
                .build();
    }

    private void updateTransactionStatus(Transaction transaction, TransactionEvent event) {
        Transaction.TransactionStatus newStatus = Transaction.TransactionStatus.valueOf(event.getStatus());
        transaction.setStatus(newStatus);

        if (newStatus == Transaction.TransactionStatus.COMPLETED ||
            newStatus == Transaction.TransactionStatus.FAILED) {
            transaction.setProcessedAt(LocalDateTime.now());
        }
    }

    private void processPixTransaction(Transaction transaction, TransactionEvent event) {
        // Simulate PIX processing logic
        log.info("Simulating PIX processing for transaction: {}", transaction.getTransactionId());

        // In a real implementation, this would:
        // 1. Validate PIX key
        // 2. Check account balances
        // 3. Call PIX provider APIs
        // 4. Update account balances
        // 5. Send notifications

        // For now, we'll just mark as processing
        if (transaction.getStatus() == Transaction.TransactionStatus.PENDING) {
            transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        }
    }
}