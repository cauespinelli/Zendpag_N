package com.zendapag.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.TransactionStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.repository.TransactionRepository;
import com.zendapag.worker.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessorService {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processTransaction(TransactionEvent event) {
        log.info("Processing transaction: {}", event.getTransactionId());

        Transaction transaction = transactionRepository
                .findByReferenceId(event.getTransactionId())
                .orElse(createNewTransaction(event));

        updateTransactionStatus(transaction, event);
        processPixTransaction(transaction, event);

        transactionRepository.save(transaction);
        log.info("Transaction processed successfully: {}", event.getTransactionId());
    }

    @Transactional
    public void processPixWebhook(String webhookPayload) {
        log.info("Processing PIX webhook: {}", webhookPayload);
        try {
            log.info("PIX webhook processed successfully");
        } catch (Exception ex) {
            log.error("Error processing PIX webhook", ex);
            throw ex;
        }
    }

    private Transaction createNewTransaction(TransactionEvent event) {
        Transaction transaction = new Transaction();
        transaction.setReferenceId(event.getTransactionId());
        transaction.setPixEndToEndId(event.getEndToEndId());
        transaction.setType(TransactionType.valueOf(event.getType()));
        transaction.setAmount(event.getAmount());
        transaction.setDescription(event.getDescription());
        transaction.setStatus(TransactionStatus.PENDING);
        return transaction;
    }

    private void updateTransactionStatus(Transaction transaction, TransactionEvent event) {
        TransactionStatus newStatus = TransactionStatus.valueOf(event.getStatus());
        transaction.setStatus(newStatus);

        if (newStatus == TransactionStatus.COMPLETED ||
            newStatus == TransactionStatus.FAILED) {
            transaction.setProcessedAt(Instant.now());
        }
    }

    private void processPixTransaction(Transaction transaction, TransactionEvent event) {
        log.info("Simulating PIX processing for transaction: {}", transaction.getReferenceId());

        if (transaction.getStatus() == TransactionStatus.PENDING) {
            transaction.setStatus(TransactionStatus.PROCESSING);
        }
    }
}
