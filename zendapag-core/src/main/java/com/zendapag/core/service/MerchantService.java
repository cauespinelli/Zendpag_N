package com.zendapag.core.service;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.entity.enums.MerchantStatus;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.repository.MerchantRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final AuditService auditService;
    private final RiskService riskService;

    @Autowired
    public MerchantService(MerchantRepository merchantRepository,
                          AuditService auditService,
                          RiskService riskService) {
        this.merchantRepository = merchantRepository;
        this.auditService = auditService;
        this.riskService = riskService;
    }

    @Timed
    @CircuitBreaker(name = "merchant-service")
    @Retry(name = "merchant-service")
    public Merchant createMerchant(Merchant merchant) {
        log.info("Creating merchant with document: {}", maskDocument(merchant.getDocument()));

        // Validate merchant data
        validateMerchantData(merchant);

        // Check for duplicate document/email
        validateUniqueFields(merchant);

        // Set initial status and defaults
        merchant.setStatus(MerchantStatus.PENDING);
        merchant.setRiskScore(0);

        try {
            // Save merchant
            Merchant savedMerchant = merchantRepository.save(merchant);

            // Audit log
            auditService.logAction(
                "MERCHANT",
                savedMerchant.getId().toString(),
                AuditAction.CREATE,
                "Merchant created"
            );

            // Perform initial risk assessment
            performInitialRiskAssessment(savedMerchant);

            log.info("Merchant created successfully: {}", savedMerchant.getId());
            return savedMerchant;

        } catch (Exception e) {
            log.error("Failed to create merchant", e);
            auditService.logFailure("MERCHANT", null, AuditAction.CREATE, e.getMessage(), e);
            throw new BusinessException("Failed to create merchant: " + e.getMessage());
        }
    }

    @Timed
    @CacheEvict(value = "merchants", key = "#merchant.id")
    public Merchant updateMerchant(Merchant merchant) {
        log.info("Updating merchant: {}", merchant.getId());

        Optional<Merchant> existingMerchant = merchantRepository.findById(merchant.getId());
        if (!existingMerchant.isPresent()) {
            throw new BusinessException("Merchant not found: " + merchant.getId());
        }

        Merchant existing = existingMerchant.get();

        // Track changes for audit
        Map<String, Object> oldValues = new HashMap<>();
        Map<String, Object> newValues = new HashMap<>();

        if (!existing.getName().equals(merchant.getName())) {
            oldValues.put("name", existing.getName());
            newValues.put("name", merchant.getName());
        }

        if (!existing.getEmail().equals(merchant.getEmail())) {
            oldValues.put("email", existing.getEmail());
            newValues.put("email", merchant.getEmail());

            // Validate email uniqueness
            if (merchantRepository.existsByEmail(merchant.getEmail())) {
                throw new BusinessException("Email already in use: " + merchant.getEmail());
            }
        }

        try {
            // Update fields
            existing.setName(merchant.getName());
            existing.setEmail(merchant.getEmail());
            existing.setPhoneNumber(merchant.getPhoneNumber());
            existing.setWebsiteUrl(merchant.getWebsiteUrl());
            existing.setDescription(merchant.getDescription());

            Merchant savedMerchant = merchantRepository.save(existing);

            // Audit log
            if (!oldValues.isEmpty()) {
                auditService.logAction(
                    "MERCHANT",
                    savedMerchant.getId().toString(),
                    AuditAction.UPDATE,
                    oldValues,
                    newValues
                );
            }

            log.info("Merchant updated successfully: {}", savedMerchant.getId());
            return savedMerchant;

        } catch (Exception e) {
            log.error("Failed to update merchant: {}", merchant.getId(), e);
            auditService.logFailure(
                "MERCHANT",
                merchant.getId().toString(),
                AuditAction.UPDATE,
                e.getMessage(),
                e
            );
            throw new BusinessException("Failed to update merchant: " + e.getMessage());
        }
    }

    @Cacheable(value = "merchants", key = "#merchantId")
    @Transactional(readOnly = true)
    @Timed
    public Optional<Merchant> findById(UUID merchantId) {
        log.debug("Finding merchant by ID: {}", merchantId);
        return merchantRepository.findById(merchantId);
    }

    @Cacheable(value = "merchants", key = "'doc-' + #document")
    @Transactional(readOnly = true)
    public Optional<Merchant> findByDocument(String document) {
        log.debug("Finding merchant by document: {}", maskDocument(document));
        return merchantRepository.findByDocument(document);
    }

    @Cacheable(value = "merchants", key = "'email-' + #email")
    @Transactional(readOnly = true)
    public Optional<Merchant> findByEmail(String email) {
        log.debug("Finding merchant by email: {}", email);
        return merchantRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Page<Merchant> findAll(Pageable pageable) {
        return merchantRepository.findAll(pageable);
    }

    @Timed
    @CacheEvict(value = "merchants", key = "#merchantId")
    public Merchant activateMerchant(UUID merchantId) {
        log.info("Activating merchant: {}", merchantId);

        Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);
        if (!merchantOpt.isPresent()) {
            throw new BusinessException("Merchant not found: " + merchantId);
        }

        Merchant merchant = merchantOpt.get();

        if (merchant.getStatus() == MerchantStatus.ACTIVE) {
            log.warn("Merchant already active: {}", merchantId);
            return merchant;
        }

        // Validate merchant is ready for activation
        validateMerchantForActivation(merchant);

        try {
            // Update status
            MerchantStatus oldStatus = merchant.getStatus();
            merchant.activate();

            Merchant savedMerchant = merchantRepository.save(merchant);

            // Audit log
            Map<String, Object> oldValues = Map.of("status", oldStatus.name());
            Map<String, Object> newValues = Map.of("status", MerchantStatus.ACTIVE.name());

            auditService.logAction(
                "MERCHANT",
                merchantId.toString(),
                AuditAction.APPROVE,
                oldValues,
                newValues
            );

            log.info("Merchant activated: {}", merchantId);
            return savedMerchant;

        } catch (Exception e) {
            log.error("Failed to activate merchant: {}", merchantId, e);
            auditService.logFailure(
                "MERCHANT",
                merchantId.toString(),
                AuditAction.APPROVE,
                e.getMessage(),
                e
            );
            throw new BusinessException("Failed to activate merchant: " + e.getMessage());
        }
    }

    @Timed
    @CacheEvict(value = "merchants", key = "#merchantId")
    public Merchant suspendMerchant(UUID merchantId, String reason) {
        log.info("Suspending merchant: {} - Reason: {}", merchantId, reason);

        Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);
        if (!merchantOpt.isPresent()) {
            throw new BusinessException("Merchant not found: " + merchantId);
        }

        Merchant merchant = merchantOpt.get();

        try {
            MerchantStatus oldStatus = merchant.getStatus();
            merchant.suspend(reason);

            Merchant savedMerchant = merchantRepository.save(merchant);

            // Audit log
            Map<String, Object> oldValues = Map.of("status", oldStatus.name());
            Map<String, Object> newValues = Map.of("status", MerchantStatus.SUSPENDED.name());

            auditService.logAction(
                "MERCHANT",
                merchantId.toString(),
                AuditAction.SUSPEND,
                reason,
                oldValues,
                newValues
            );

            log.info("Merchant suspended: {}", merchantId);
            return savedMerchant;

        } catch (Exception e) {
            log.error("Failed to suspend merchant: {}", merchantId, e);
            auditService.logFailure(
                "MERCHANT",
                merchantId.toString(),
                AuditAction.SUSPEND,
                e.getMessage(),
                e
            );
            throw new BusinessException("Failed to suspend merchant: " + e.getMessage());
        }
    }

    @Timed
    @CacheEvict(value = "merchants", key = "#merchantId")
    public Merchant verifyKyc(UUID merchantId) {
        log.info("Verifying KYC for merchant: {}", merchantId);

        Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);
        if (!merchantOpt.isPresent()) {
            throw new BusinessException("Merchant not found: " + merchantId);
        }

        Merchant merchant = merchantOpt.get();

        if (merchant.isKycVerified()) {
            log.warn("Merchant KYC already verified: {}", merchantId);
            return merchant;
        }

        try {
            merchant.verifyKyc();
            Merchant savedMerchant = merchantRepository.save(merchant);

            // Update risk score after KYC verification
            riskService.evaluateMerchantRisk(savedMerchant);

            // Audit log
            auditService.logAction(
                "MERCHANT",
                merchantId.toString(),
                AuditAction.VERIFY,
                "KYC verified"
            );

            log.info("Merchant KYC verified: {}", merchantId);
            return savedMerchant;

        } catch (Exception e) {
            log.error("Failed to verify merchant KYC: {}", merchantId, e);
            auditService.logFailure(
                "MERCHANT",
                merchantId.toString(),
                AuditAction.VERIFY,
                e.getMessage(),
                e
            );
            throw new BusinessException("Failed to verify KYC: " + e.getMessage());
        }
    }

    @RateLimiter(name = "merchant-service")
    public void updateLastLogin(UUID merchantId) {
        try {
            merchantRepository.updateLastLogin(merchantId, Instant.now());
            log.debug("Last login updated for merchant: {}", merchantId);
        } catch (Exception e) {
            log.warn("Failed to update last login for merchant: {}", merchantId);
        }
    }

    // Private helper methods

    private void validateMerchantData(Merchant merchant) {
        if (merchant.getDocument() == null || merchant.getDocument().trim().isEmpty()) {
            throw new BusinessException("Merchant document is required");
        }

        if (merchant.getEmail() == null || merchant.getEmail().trim().isEmpty()) {
            throw new BusinessException("Merchant email is required");
        }

        if (merchant.getName() == null || merchant.getName().trim().isEmpty()) {
            throw new BusinessException("Merchant name is required");
        }

        // Validate document format
        if (!isValidDocument(merchant.getDocument())) {
            throw new BusinessException("Invalid document format");
        }
    }

    private void validateUniqueFields(Merchant merchant) {
        if (merchantRepository.existsByDocument(merchant.getDocument())) {
            throw new BusinessException("Merchant document already registered");
        }

        if (merchantRepository.existsByEmail(merchant.getEmail())) {
            throw new BusinessException("Merchant email already registered");
        }
    }

    private void validateMerchantForActivation(Merchant merchant) {
        if (!merchant.isKycVerified()) {
            throw new BusinessException("Merchant must complete KYC verification before activation");
        }

        if (merchant.getRiskScore() > 80) {
            throw new BusinessException("Merchant risk score too high for activation");
        }
    }

    private void performInitialRiskAssessment(Merchant merchant) {
        try {
            riskService.evaluateMerchantRisk(merchant);
        } catch (Exception e) {
            log.warn("Initial risk assessment failed for merchant {}: {}", merchant.getId(), e.getMessage());
        }
    }

    private boolean isValidDocument(String document) {
        String cleanDocument = document.replaceAll("[^0-9]", "");

        if (cleanDocument.length() == 11) {
            return isValidCPF(cleanDocument);
        } else if (cleanDocument.length() == 14) {
            return isValidCNPJ(cleanDocument);
        }

        return false;
    }

    private boolean isValidCPF(String cpf) {
        // Basic CPF validation logic
        return cpf.length() == 11 && !cpf.matches("(\\d)\\1{10}");
    }

    private boolean isValidCNPJ(String cnpj) {
        // Basic CNPJ validation logic
        return cnpj.length() == 14 && !cnpj.matches("(\\d)\\1{13}");
    }

    private String maskDocument(String document) {
        if (document == null || document.length() < 6) {
            return "***";
        }
        return document.substring(0, 3) + "***" + document.substring(document.length() - 2);
    }

    // Fallback methods for Circuit Breaker
    public Merchant fallbackCreateMerchant(Merchant merchant, Exception ex) {
        log.error("Fallback: Merchant creation failed: {}", ex.getMessage());
        throw new BusinessException("Merchant service temporarily unavailable");
    }
}
