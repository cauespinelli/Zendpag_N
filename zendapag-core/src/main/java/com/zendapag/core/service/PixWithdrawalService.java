package com.zendapag.core.service;
import lombok.extern.slf4j.Slf4j;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.dto.request.CreatePixWithdrawalRequest;
import com.zendapag.core.dto.response.PixWithdrawalResponse;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.entity.enums.WithdrawalStatus;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PixWithdrawalRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service para processamento de saques PIX
 */
@Service
@Transactional
@Slf4j
@Slf4j
public class PixWithdrawalService {

    private final PixWithdrawalRepository withdrawalRepository;
    private final AccountRepository accountRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionService transactionService;
    private final PixService pixService;
    private final RiskService riskService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${zendapag.withdrawal.max-amount:50000.00}")
    private BigDecimal maxWithdrawalAmount;

    @Value("${zendapag.withdrawal.min-amount:0.01}")
    private BigDecimal minWithdrawalAmount;

    @Value("${zendapag.withdrawal.daily-limit:100000.00}")
    private BigDecimal dailyWithdrawalLimit;

    @Value("${zendapag.withdrawal.fee-percentage:0.00}")
    private BigDecimal feePercentage;

    @Value("${zendapag.withdrawal.fixed-fee:0.00}")
    private BigDecimal fixedFee;

    @Value("${zendapag.withdrawal.max-pending:5}")
    private int maxPendingWithdrawals;

    @Autowired
    public PixWithdrawalService(PixWithdrawalRepository withdrawalRepository,
                               AccountRepository accountRepository,
                               MerchantRepository merchantRepository,
                               TransactionService transactionService,
                               PixService pixService,
                               RiskService riskService,
                               AuditService auditService,
                               ApplicationEventPublisher eventPublisher,
                               KafkaTemplate<String, Object> kafkaTemplate) {
        this.withdrawalRepository = withdrawalRepository;
        this.accountRepository = accountRepository;
        this.merchantRepository = merchantRepository;
        this.transactionService = transactionService;
        this.pixService = pixService;
        this.riskService = riskService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Cria uma solicitação de saque PIX
     */
    @Timed
    @CircuitBreaker(name = "withdrawal")
    @RateLimiter(name = "withdrawal")
    @Retry(name = "withdrawal")
    @CacheEvict(value = "withdrawals", allEntries = true)
    public PixWithdrawalResponse createWithdrawal(UUID accountId, UUID merchantId,
                                                  CreatePixWithdrawalRequest request) {
        log.info("Creating PIX withdrawal for account: {}, amount: {}", accountId, request.getAmount());

        try {
            // 1. Validar conta e merchant
            Account account = validateAccount(accountId);
            Merchant merchant = validateMerchant(merchantId);

            // 2. Validar chave PIX
            validatePixKey(request.getPixKey(), request.getPixKeyType());

            // 3. Validar valor
            validateAmount(request.getAmount());

            // 4. Validar limites
            validateWithdrawalLimits(account, request.getAmount());

            // 5. Validar saldo
            validateBalance(account, request.getAmount());

            // 6. Verificar limites de saques pendentes
            validatePendingWithdrawals(account);

            // 7. Criar entidade de saque
            PixWithdrawal withdrawal = createWithdrawalEntity(account, merchant, request);

            // 8. Calcular taxas
            calculateFees(withdrawal);

            // 9. Análise de risco
            performRiskAnalysis(withdrawal);

            // 10. Atualizar saldo
            updateAccountBalance(account, withdrawal);

            // 11. Salvar saque
            PixWithdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

            // 12. Criar transação financeira
            Transaction transaction = transactionService.createWithdrawalTransaction(savedWithdrawal);
            savedWithdrawal.setTransaction(transaction);
            savedWithdrawal = withdrawalRepository.save(savedWithdrawal);

            // 13. Audit log
            auditService.logAction("SYSTEM", "PixWithdrawal", savedWithdrawal.getId().toString(),
                AuditAction.CREATE, "PIX withdrawal created");

            // 14. Enviar para processamento assíncrono
            sendToProcessingQueue(savedWithdrawal);

            log.info("PIX withdrawal created successfully: {}", savedWithdrawal.getReferenceId());
            return convertToResponse(savedWithdrawal);

        } catch (BusinessException e) {
            log.warn("Business validation failed for withdrawal: {}", e.getMessage());
            auditService.logFailure("SYSTEM", "PixWithdrawal", accountId.toString(),
                AuditAction.CREATE, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating withdrawal: {}", e.getMessage(), e);
            auditService.logFailure("SYSTEM", "PixWithdrawal", accountId.toString(),
                AuditAction.CREATE, e.getMessage(), e);
            throw new BusinessException("Failed to create withdrawal", e);
        }
    }

    /**
     * Processa um saque PIX (chamado pelo worker)
     */
    @Timed
    @Transactional
    public PixWithdrawalResponse processWithdrawal(UUID withdrawalId) {
        log.info("Processing withdrawal: {}", withdrawalId);

        PixWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
            .orElseThrow(() -> new BusinessException("Withdrawal not found: " + withdrawalId));

        try {
            // Marcar como em processamento
            withdrawal.startProcessing();
            withdrawalRepository.save(withdrawal);

            // Integrar com PIX para realizar transferência
            String endToEndId = processPixTransfer(withdrawal);

            // Marcar como concluído
            withdrawal.complete(endToEndId);
            withdrawal = withdrawalRepository.save(withdrawal);

            // Audit log
            auditService.logAction("SYSTEM", "PixWithdrawal", withdrawal.getId().toString(),
                AuditAction.UPDATE, "PIX withdrawal completed");

            log.info("Withdrawal processed successfully: {}", withdrawal.getReferenceId());
            return convertToResponse(withdrawal);

        } catch (Exception e) {
            log.error("Error processing withdrawal {}: {}", withdrawalId, e.getMessage(), e);
            withdrawal.markAsFailed(e.getMessage());
            withdrawalRepository.save(withdrawal);

            auditService.logFailure("SYSTEM", "PixWithdrawal", withdrawal.getId().toString(),
                AuditAction.UPDATE, e.getMessage(), e);

            throw new BusinessException("Failed to process withdrawal", e);
        }
    }

    /**
     * Cancela um saque
     */
    @Timed
    @Transactional
    @CacheEvict(value = "withdrawals", key = "#withdrawalId")
    public PixWithdrawalResponse cancelWithdrawal(UUID withdrawalId, String reason) {
        log.info("Cancelling withdrawal: {}", withdrawalId);

        PixWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
            .orElseThrow(() -> new BusinessException("Withdrawal not found: " + withdrawalId));

        if (!withdrawal.canBeCancelled()) {
            throw new BusinessException("Withdrawal cannot be cancelled in status: " + withdrawal.getStatus());
        }

        // Reverter saldo
        revertAccountBalance(withdrawal);

        // Cancelar saque
        withdrawal.cancel(reason);
        withdrawal = withdrawalRepository.save(withdrawal);

        // Audit log
        auditService.logAction("SYSTEM", "PixWithdrawal", withdrawal.getId().toString(),
            AuditAction.UPDATE, "Withdrawal cancelled: " + reason);

        log.info("Withdrawal cancelled: {}", withdrawal.getReferenceId());
        return convertToResponse(withdrawal);
    }

    /**
     * Busca saque por ID
     */
    @Cacheable(value = "withdrawals", key = "#id")
    @Transactional(readOnly = true)
    public Optional<PixWithdrawal> findById(UUID id) {
        return withdrawalRepository.findById(id);
    }

    /**
     * Busca saque por referência
     */
    @Cacheable(value = "withdrawals", key = "#referenceId")
    @Transactional(readOnly = true)
    public Optional<PixWithdrawal> findByReferenceId(String referenceId) {
        return withdrawalRepository.findByReferenceId(referenceId);
    }

    /**
     * Lista saques por conta
     */
    @Transactional(readOnly = true)
    public Page<PixWithdrawalResponse> findByAccount(UUID accountId, Pageable pageable) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new BusinessException("Account not found: " + accountId));

        return withdrawalRepository.findByAccount(account, pageable)
            .map(this::convertToResponse);
    }

    /**
     * Lista saques por merchant
     */
    @Transactional(readOnly = true)
    public Page<PixWithdrawalResponse> findByMerchant(UUID merchantId, Pageable pageable) {
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new BusinessException("Merchant not found: " + merchantId));

        return withdrawalRepository.findByMerchant(merchant, pageable)
            .map(this::convertToResponse);
    }

    /**
     * Lista saques por status
     */
    @Transactional(readOnly = true)
    public Page<PixWithdrawalResponse> findByStatus(WithdrawalStatus status, Pageable pageable) {
        return withdrawalRepository.findByStatus(status).stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .map(this::convertToResponse)
            .collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(),
                list -> new org.springframework.data.domain.PageImpl<>(list, pageable,
                    withdrawalRepository.findByStatus(status).size())
            ));
    }

    // Private helper methods

    private Account validateAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new BusinessException("Account not found: " + accountId));

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BusinessException("Account is not active");
        }

        return account;
    }

    private Merchant validateMerchant(UUID merchantId) {
        return merchantRepository.findById(merchantId)
            .orElseThrow(() -> new BusinessException("Merchant not found: " + merchantId));
    }

    private void validatePixKey(String pixKey, String pixKeyType) {
        if (pixKey == null || pixKey.isBlank()) {
            throw new BusinessException("PIX key is required");
        }

        // Validar formato da chave PIX
        boolean isValid = pixService.validatePixKey(pixKey, pixKeyType);
        if (!isValid) {
            throw new BusinessException("Invalid PIX key format");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(minWithdrawalAmount) < 0) {
            throw new BusinessException("Amount below minimum: " + minWithdrawalAmount);
        }

        if (amount.compareTo(maxWithdrawalAmount) > 0) {
            throw new BusinessException("Amount exceeds maximum: " + maxWithdrawalAmount);
        }
    }

    private void validateWithdrawalLimits(Account account, BigDecimal amount) {
        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayEnd = todayStart.plus(1, ChronoUnit.DAYS);

        BigDecimal todayWithdrawals = withdrawalRepository
            .sumCompletedWithdrawalsByAccountAndDateRange(account, todayStart, todayEnd);

        if (todayWithdrawals == null) {
            todayWithdrawals = BigDecimal.ZERO;
        }

        if (todayWithdrawals.add(amount).compareTo(dailyWithdrawalLimit) > 0) {
            throw new BusinessException("Daily withdrawal limit exceeded");
        }
    }

    private void validateBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance");
        }
    }

    private void validatePendingWithdrawals(Account account) {
        long pendingCount = withdrawalRepository.countPendingWithdrawalsByAccount(account);

        if (pendingCount >= maxPendingWithdrawals) {
            throw new BusinessException("Maximum pending withdrawals exceeded");
        }
    }

    private PixWithdrawal createWithdrawalEntity(Account account, Merchant merchant,
                                                CreatePixWithdrawalRequest request) {
        String referenceId = "WD-" + UUID.randomUUID().toString();

        PixWithdrawal withdrawal = new PixWithdrawal(
            referenceId,
            account,
            merchant,
            request.getAmount(),
            request.getPixKey(),
            request.getPixKeyType()
        );

        withdrawal.setDescription(request.getDescription());
        withdrawal.setExternalReference(request.getExternalReference());
        withdrawal.setRecipientName(request.getRecipientName());
        withdrawal.setRecipientDocument(request.getRecipientDocument());
        withdrawal.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));

        return withdrawal;
    }

    private void calculateFees(PixWithdrawal withdrawal) {
        BigDecimal fee = withdrawal.getAmount()
            .multiply(feePercentage.divide(BigDecimal.valueOf(100)))
            .add(fixedFee);

        withdrawal.setFeeAmount(fee);
    }

    private void performRiskAnalysis(PixWithdrawal withdrawal) {
        // Implementar análise de risco aqui
        // Por enquanto, apenas log
        log.debug("Performing risk analysis for withdrawal: {}", withdrawal.getReferenceId());
    }

    private void updateAccountBalance(Account account, PixWithdrawal withdrawal) {
        BigDecimal previousBalance = account.getBalance();
        account.setBalance(previousBalance.subtract(withdrawal.getAmount()));
        accountRepository.save(account);

        withdrawal.updateBalance(previousBalance);
    }

    private void revertAccountBalance(PixWithdrawal withdrawal) {
        Account account = withdrawal.getAccount();
        BigDecimal currentBalance = account.getBalance();
        account.setBalance(currentBalance.add(withdrawal.getAmount()));
        accountRepository.save(account);
    }

    private void sendToProcessingQueue(PixWithdrawal withdrawal) {
        try {
            kafkaTemplate.send("withdrawal-events", withdrawal.getId().toString(), withdrawal);
            log.debug("Withdrawal sent to processing queue: {}", withdrawal.getReferenceId());
        } catch (Exception e) {
            log.error("Failed to send withdrawal to queue: {}", e.getMessage(), e);
        }
    }

    private String processPixTransfer(PixWithdrawal withdrawal) {
        // Integrar com provedor PIX para realizar transferência
        // Por enquanto, simular sucesso
        String endToEndId = "E" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        log.info("PIX transfer simulated for withdrawal: {}, endToEndId: {}",
            withdrawal.getReferenceId(), endToEndId);
        return endToEndId;
    }

    public PixWithdrawalResponse convertToResponse(PixWithdrawal withdrawal) {
        PixWithdrawalResponse response = new PixWithdrawalResponse();
        response.setId(withdrawal.getId());
        response.setReferenceId(withdrawal.getReferenceId());
        response.setAmount(withdrawal.getAmount());
        response.setFeeAmount(withdrawal.getFeeAmount());
        response.setNetAmount(withdrawal.getNetAmount());
        response.setPixKey(withdrawal.getPixKey());
        response.setPixKeyType(withdrawal.getPixKeyType());
        response.setRecipientName(withdrawal.getRecipientName());
        response.setRecipientDocument(withdrawal.getRecipientDocument());
        response.setRecipientBank(withdrawal.getRecipientBank());
        response.setStatus(withdrawal.getStatus());
        response.setDescription(withdrawal.getDescription());
        response.setRejectionReason(withdrawal.getRejectionReason());
        response.setPixTransactionId(withdrawal.getPixTransactionId());
        response.setPixEndToEndId(withdrawal.getPixEndToEndId());
        response.setRequestedAt(withdrawal.getRequestedAt());
        response.setProcessedAt(withdrawal.getProcessedAt());
        response.setCompletedAt(withdrawal.getCompletedAt());
        response.setCancelledAt(withdrawal.getCancelledAt());
        response.setExpiresAt(withdrawal.getExpiresAt());
        response.setBalanceBefore(withdrawal.getBalanceBefore());
        response.setBalanceAfter(withdrawal.getBalanceAfter());
        response.setMetadata(withdrawal.getMetadata());
        response.setCreatedAt(withdrawal.getCreatedAt());
        response.setUpdatedAt(withdrawal.getUpdatedAt());
        return response;
    }
}
