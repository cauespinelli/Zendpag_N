package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.TransactionStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction createTransaction(Payment payment, TransactionType type, BigDecimal amount) {
        log.info("Creating transaction for payment: {} type: {} amount: {}",
            payment.getReferenceId(), type, amount);

        Transaction transaction = new Transaction();
        transaction.setReferenceId(generateReferenceId());
        transaction.setMerchant(payment.getMerchant());
        transaction.setAccount(payment.getAccount());
        transaction.setPayment(payment);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setCurrency(payment.getCurrency());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDescription("Transaction for payment: " + payment.getReferenceId());
        transaction.setCreatedAt(Instant.now());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created with ID: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Transaction createCreditTransaction(Account account, BigDecimal amount, String description) {
        log.info("Creating credit transaction for account: {} amount: {}",
            account.getId(), amount);

        Transaction transaction = new Transaction();
        transaction.setReferenceId(generateReferenceId());
        transaction.setMerchant(account.getMerchant());
        transaction.setAccount(account);
        transaction.setType(TransactionType.CREDIT);
        transaction.setAmount(amount);
        transaction.setCurrency("BRL");
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription(description);
        transaction.setCreatedAt(Instant.now());
        transaction.setProcessedAt(Instant.now());

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createDebitTransaction(Account account, BigDecimal amount, String description) {
        log.info("Creating debit transaction for account: {} amount: {}",
            account.getId(), amount);

        Transaction transaction = new Transaction();
        transaction.setReferenceId(generateReferenceId());
        transaction.setMerchant(account.getMerchant());
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEBIT);
        transaction.setAmount(amount);
        transaction.setCurrency("BRL");
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription(description);
        transaction.setCreatedAt(Instant.now());
        transaction.setProcessedAt(Instant.now());

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Transaction findById(UUID id) {
        return transactionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
    }

    @Transactional(readOnly = true)
    public Transaction findByReferenceId(String referenceId) {
        return transactionRepository.findByReferenceId(referenceId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", "referenceId", referenceId));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> findByMerchant(Merchant merchant, Pageable pageable) {
        return transactionRepository.findByMerchant(merchant, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> findByAccount(Account account, Pageable pageable) {
        return transactionRepository.findByAccount(account, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> findByMerchantAndType(Merchant merchant, TransactionType type, Pageable pageable) {
        return transactionRepository.findByMerchantAndType(merchant, type, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> findByMerchantAndStatus(Merchant merchant, TransactionStatus status, Pageable pageable) {
        return transactionRepository.findByMerchantAndStatus(merchant, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> findByMerchantAndCreatedAtBetween(Merchant merchant, Instant start, Instant end, Pageable pageable) {
        return transactionRepository.findByMerchantAndCreatedAtBetween(merchant, start, end, pageable);
    }

    @Transactional
    public Transaction updateStatus(UUID id, TransactionStatus status) {
        Transaction transaction = findById(id);
        transaction.setStatus(status);
        if (status == TransactionStatus.COMPLETED || status == TransactionStatus.FAILED) {
            transaction.setProcessedAt(Instant.now());
        }
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction completeTransaction(UUID id) {
        Transaction transaction = findById(id);
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("INVALID_TRANSACTION_STATUS",
                "Transaction must be in PENDING status to complete");
        }
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setProcessedAt(Instant.now());
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction failTransaction(UUID id, String errorMessage) {
        Transaction transaction = findById(id);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setProcessedAt(Instant.now());
        transaction.setErrorMessage(errorMessage);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction reverseTransaction(UUID id, String reason) {
        Transaction originalTransaction = findById(id);

        if (originalTransaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new BusinessException("INVALID_TRANSACTION_STATUS",
                "Only completed transactions can be reversed");
        }

        Transaction reversal = new Transaction();
        reversal.setReferenceId(generateReferenceId());
        reversal.setMerchant(originalTransaction.getMerchant());
        reversal.setAccount(originalTransaction.getAccount());
        reversal.setPayment(originalTransaction.getPayment());
        reversal.setType(TransactionType.REVERSAL);
        reversal.setAmount(originalTransaction.getAmount());
        reversal.setCurrency(originalTransaction.getCurrency());
        reversal.setStatus(TransactionStatus.COMPLETED);
        reversal.setDescription("Reversal of transaction: " + originalTransaction.getReferenceId() + " - " + reason);
        reversal.setCreatedAt(Instant.now());
        reversal.setProcessedAt(Instant.now());
        reversal.setOriginalTransactionId(originalTransaction.getId());

        originalTransaction.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(originalTransaction);

        return transactionRepository.save(reversal);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Account account) {
        BigDecimal credits = transactionRepository.sumCreditsByAccount(account);
        BigDecimal debits = transactionRepository.sumDebitsByAccount(account);

        credits = credits != null ? credits : BigDecimal.ZERO;
        debits = debits != null ? debits : BigDecimal.ZERO;

        return credits.subtract(debits);
    }

    @Transactional(readOnly = true)
    public long countByMerchantAndCreatedAtBetween(Merchant merchant, Instant start, Instant end) {
        return transactionRepository.countByMerchantAndCreatedAtBetween(merchant, start, end);
    }

    private String generateReferenceId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional(readOnly = true)
    public BigDecimal getMerchantBalance(Merchant merchant) {
        log.debug("Getting balance for merchant: {}", merchant.getId());
        BigDecimal balance = transactionRepository.calculateMerchantBalance(merchant);
        return balance != null ? balance : BigDecimal.ZERO;
    }

}