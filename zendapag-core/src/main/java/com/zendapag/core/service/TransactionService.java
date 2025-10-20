package com.zendapag.core.service;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Settlement;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.repository.TransactionRepository;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                             AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    @Timed
    @Transactional
    public Transaction createPaymentTransaction {
        log.info);

        try {
            // Calculate current balance
            BigDecimal currentBalance = getMerchantBalance);

            // Create credit transaction for net amount
            Transaction transaction = new Transaction;
            transaction.setMerchant);
            transaction.setPayment;
            transaction.setReferenceId));
            transaction.setType;
            transaction.setCategory;
            transaction.setAmount);
            transaction.setCurrency);
            transaction.setDescription);
            transaction.setBalanceBefore;
            transaction.setBalanceAfter));
            transaction.setProcessedAt);
            transaction.setSettlementDate.plusDays(1)); // T+1 settlement

            Transaction savedTransaction = transactionRepository.save;

            // Create fee transaction if applicable
            if .compareTo(BigDecimal.ZERO) > 0) {
                createFeeTransaction;
            }

            // Audit log
            auditService.logAction, "Transaction", savedTransaction.getId().toString(),
                AuditAction.CREATE, "Payment transaction created");

            log.info("Payment transaction created: {} for payment: {}",
                savedTransaction.getReferenceId, payment.getReferenceId());

            return savedTransaction;

        } catch  {
            log.error, e.getMessage(), e);
            auditService.logFailure, "Transaction", payment.getId().toString(),
                AuditAction.CREATE, "Payment transaction creation failed", e);
            throw new BusinessException;
        }
    }

    @Timed
    public Transaction confirmPaymentTransaction {
        log.info);

        try {
            List<Transaction> paymentTransactions = transactionRepository.findByPayment;
            if ) {
                throw new BusinessException;
            }

            Transaction transaction = paymentTransactions.get; // Main payment transaction
            transaction.setProcessedAt);
            transaction.setSettled; // Will be settled later

            Transaction savedTransaction = transactionRepository.save;

            // Audit log
            auditService.logAction, "Transaction", savedTransaction.getId().toString(),
                AuditAction.UPDATE, "Payment transaction confirmed");

            return savedTransaction;

        } catch  {
            log.error, e.getMessage(), e);
            throw new BusinessException;
        }
    }

    @Timed
    @Transactional
    public Transaction reversePaymentTransaction {
        log.info, reason);

        try {
            List<Transaction> paymentTransactions = transactionRepository.findByPayment;
            if ) {
                throw new BusinessException;
            }

            Transaction originalTransaction = paymentTransactions.get;

            // Check if already reversed
            if  != null) {
                log.warn);
                return originalTransaction;
            }

            // Calculate current balance
            BigDecimal currentBalance = getMerchantBalance);

            // Create reversal transaction
            Transaction reversalTransaction = new Transaction;
            reversalTransaction.setMerchant);
            reversalTransaction.setPayment;
            reversalTransaction.setReferenceId));
            reversalTransaction.setType;
            reversalTransaction.setCategory;
            reversalTransaction.setAmount);
            reversalTransaction.setCurrency);
            reversalTransaction.setDescription;
            reversalTransaction.setParentTransactionId);
            reversalTransaction.setBalanceBefore;
            reversalTransaction.setBalanceAfter));
            reversalTransaction.setProcessedAt);

            Transaction savedReversalTransaction = transactionRepository.save;

            // Update original transaction
            originalTransaction.setReversalTransactionId);
            transactionRepository.save;

            // Audit log
            Map<String, Object> metadata = Map.of);
            auditService.logAction, "Transaction", savedReversalTransaction.getId().toString(),
                AuditAction.CREATE, "Payment transaction reversed: " + reason, null, metadata);

            log.info("Payment transaction reversed: {} for payment: {}",
                savedReversalTransaction.getReferenceId, payment.getReferenceId());

            return savedReversalTransaction;

        } catch  {
            log.error, e.getMessage(), e);
            auditService.logFailure, "Transaction", payment.getId().toString(),
                AuditAction.CREATE, "Payment transaction reversal failed", e);
            throw new BusinessException;
        }
    }

    @Timed
    @Transactional
    public Transaction createRefundTransaction {
        log.info, refundAmount);

        try {
            // Calculate current balance
            BigDecimal currentBalance = getMerchantBalance);

            // Create refund transaction 
            Transaction transaction = new Transaction;
            transaction.setMerchant);
            transaction.setPayment;
            transaction.setReferenceId));
            transaction.setType;
            transaction.setCategory;
            transaction.setAmount;
            transaction.setCurrency);
            transaction.setDescription;
            transaction.setBalanceBefore;
            transaction.setBalanceAfter);
            transaction.setProcessedAt);
            transaction.setSettlementDate.plusDays(1));

            Transaction savedTransaction = transactionRepository.save;

            // Audit log
            auditService.logAction, "Transaction", savedTransaction.getId().toString(),
                AuditAction.CREATE, "Refund transaction created: " + reason);

            log.info("Refund transaction created: {} for payment: {}",
                savedTransaction.getReferenceId, payment.getReferenceId());

            return savedTransaction;

        } catch  {
            log.error, e.getMessage(), e);
            auditService.logFailure, "Transaction", payment.getId().toString(),
                AuditAction.CREATE, "Refund transaction creation failed", e);
            throw new BusinessException;
        }
    }

    @Timed
    @Transactional
    public Transaction createSettlementTransaction {
        log.info);

        try {
            // Calculate current balance
            BigDecimal currentBalance = getMerchantBalance);

            // Calculate settlement amount 
            BigDecimal settlementAmount = settlement.getNetAmount;

            // Create settlement transaction 
            Transaction transaction = new Transaction;
            transaction.setMerchant);
            transaction.setSettlement;
            transaction.setReferenceId.toString()));
            transaction.setType;
            transaction.setCategory;
            transaction.setAmount;
            transaction.setCurrency);
            transaction.setDescription + " transactions");
            transaction.setBalanceBefore;
            transaction.setBalanceAfter);
            transaction.setProcessedAt);
            transaction.setSettled;
            transaction.setSettlementDate);

            Transaction savedTransaction = transactionRepository.save;

            // Mark settled transactions as settled
            markTransactionsAsSettled;

            // Audit log
            Map<String, Object> metadata = Map.of(
                "settlement_id", settlement.getId,
                "transactions_count", settledTransactions.size,
                "settlement_amount", settlementAmount
            );
            auditService.logAction, "Transaction", savedTransaction.getId().toString(),
                AuditAction.CREATE, "Settlement transaction created", null, metadata);

            log.info("Settlement transaction created: {} for {} transactions",
                savedTransaction.getReferenceId, settledTransactions.size());

            return savedTransaction;

        } catch  {
            log.error, e);
            auditService.logFailure, "Transaction", settlement.getId().toString(),
                AuditAction.CREATE, "Settlement transaction creation failed", e);
            throw new BusinessException;
        }
    }

    @Timed
    @Transactional
    public Transaction createAdjustmentTransaction {
        log.info("Creating adjustment transaction for merchant: {} amount: {} type: {}",
            merchant.getDocument, amount, type);

        try {
            // Calculate current balance
            BigDecimal currentBalance = getMerchantBalance;

            // Create adjustment transaction
            Transaction transaction = new Transaction;
            transaction.setMerchant;
            transaction.setReferenceId.toString()));
            transaction.setType;
            transaction.setCategory;
            transaction.setAmount); // Always positive
            transaction.setCurrency;
            transaction.setDescription;
            transaction.setBalanceBefore;

            if  {
                transaction.setBalanceAfter));
            } else {
                transaction.setBalanceAfter));
            }

            transaction.setProcessedAt);
            transaction.setSettled; // Adjustments are immediately settled

            Transaction savedTransaction = transactionRepository.save;

            // Audit log
            auditService.logAction.toString(),
                AuditAction.CREATE, "Adjustment transaction created: " + reason);

            log.info("Adjustment transaction created: {} for merchant: {}",
                savedTransaction.getReferenceId, merchant.getDocument());

            return savedTransaction;

        } catch  {
            log.error, e);
            auditService.logFailure.toString(),
                AuditAction.CREATE, "Adjustment transaction creation failed", e);
            throw new BusinessException;
        }
    }

    @Cacheable
    @Transactional
    @Timed
    public BigDecimal getMerchantBalance {
        log.debug);

        BigDecimal balance = transactionRepository.calculateMerchantBalance;
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Transactional
    public BigDecimal getMerchantBalanceUpTo {
        BigDecimal balance = transactionRepository.calculateMerchantBalanceUpTo;
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Transactional
    public List<Transaction> getUnsettledTransactions {
        return transactionRepository.findUnsettledCreditsByMerchant;
    }

    @Transactional
    public List<Transaction> getUnsettledTransactionsBefore {
        return transactionRepository.findUnsettledCreditsCreatedBefore;
    }

    @Cacheable
    @Transactional
    public Optional<Transaction> findById {
        return transactionRepository.findById;
    }

    @Transactional
    public Optional<Transaction> findByReferenceId {
        return transactionRepository.findByReferenceId;
    }

    @Transactional
    public Page<Transaction> findByMerchant {
        return transactionRepository.findByMerchant;
    }

    @Transactional
    public Page<Transaction> findAll {
        return transactionRepository.findAll;
    }

    @CacheEvict
    private void invalidateMerchantBalanceCache {
        log.debug);
    }

    private Transaction createFeeTransaction {
        log.debug);

        try {
            BigDecimal currentBalance = getMerchantBalance);

            Transaction feeTransaction = new Transaction;
            feeTransaction.setMerchant);
            feeTransaction.setPayment;
            feeTransaction.setReferenceId));
            feeTransaction.setType;
            feeTransaction.setCategory;
            feeTransaction.setAmount);
            feeTransaction.setCurrency);
            feeTransaction.setDescription);
            feeTransaction.setParentTransactionId);
            feeTransaction.setBalanceBefore));
            feeTransaction.setBalanceAfter).subtract(payment.getFeeAmount()));
            feeTransaction.setProcessedAt);
            feeTransaction.setSettlementDate);

            return transactionRepository.save;

        } catch  {
            log.warn, e.getMessage());
            throw e;
        }
    }

    private void markTransactionsAsSettled {
        try {
            for  {
                transaction.setSettled;
                transaction.setSettlement;
                transactionRepository.save;
            }
        } catch  {
            log.error, e);
            throw new BusinessException;
        }
    }

    private String generateTransactionReference {
        String timestamp = String.valueOf);
        return prefix + "_" + timestamp + "_" + reference.replaceAll.substring(0, Math.min(reference.length(), 10));
    }
}