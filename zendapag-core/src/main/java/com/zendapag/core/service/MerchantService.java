package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.enums.MerchantStatus;
import com.zendapag.core.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    @Transactional
    public Merchant createMerchant(Merchant merchant) {
        log.info("Creating merchant: {}", merchant.getName());

        validateMerchant(merchant);

        if (merchant.getStatus() == null) {
            merchant.setStatus(MerchantStatus.PENDING_APPROVAL);
        }
        if (merchant.getFeeRate() == null) {
            merchant.setFeeRate(new BigDecimal("1.99"));
        }

        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant created with ID: {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Merchant findById(UUID id) {
        return merchantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", id));
    }

    @Transactional(readOnly = true)
    public Merchant findByDocument(String document) {
        return merchantRepository.findByDocument(document)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", "document", document));
    }

    @Transactional(readOnly = true)
    public Optional<Merchant> findByDocumentOptional(String document) {
        return merchantRepository.findByDocument(document);
    }

    @Transactional(readOnly = true)
    public Merchant findByApiKeyHash(String keyHash) {
        return merchantRepository.findByApiKeyHash(keyHash)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", "apiKey", "****"));
    }

    @Transactional(readOnly = true)
    public Page<Merchant> findAll(Pageable pageable) {
        return merchantRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Merchant> findByStatus(MerchantStatus status) {
        return merchantRepository.findByStatus(status);
    }

    @Transactional
    public Merchant updateMerchant(UUID id, Merchant merchantData) {
        Merchant merchant = findById(id);

        if (merchantData.getName() != null) {
            merchant.setName(merchantData.getName());
        }
        if (merchantData.getTradingName() != null) {
            merchant.setTradingName(merchantData.getTradingName());
        }
        if (merchantData.getEmail() != null) {
            merchant.setEmail(merchantData.getEmail());
        }
        if (merchantData.getPhone() != null) {
            merchant.setPhone(merchantData.getPhone());
        }
        if (merchantData.getWebhookUrl() != null) {
            merchant.setWebhookUrl(merchantData.getWebhookUrl());
        }
        if (merchantData.getFeeRate() != null) {
            merchant.setFeeRate(merchantData.getFeeRate());
        }

        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant updateStatus(UUID id, MerchantStatus status) {
        Merchant merchant = findById(id);
        MerchantStatus oldStatus = merchant.getStatus();

        merchant.setStatus(status);

        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant {} status updated from {} to {}", id, oldStatus, status);
        return saved;
    }

    @Transactional
    public Merchant activateMerchant(UUID id) {
        return updateStatus(id, MerchantStatus.ACTIVE);
    }

    @Transactional
    public Merchant suspendMerchant(UUID id, String reason) {
        Merchant merchant = findById(id);
        merchant.setStatus(MerchantStatus.SUSPENDED);
        merchant.setNotes(reason);
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant updateWebhookUrl(UUID id, String webhookUrl) {
        Merchant merchant = findById(id);
        merchant.setWebhookUrl(webhookUrl);
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant updateFees(UUID id, BigDecimal feeRate) {
        Merchant merchant = findById(id);

        if (feeRate != null) {
            if (feeRate.compareTo(BigDecimal.ZERO) < 0 || feeRate.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException("INVALID_FEE", "Fee rate must be between 0 and 100");
            }
            merchant.setFeeRate(feeRate);
        }

        return merchantRepository.save(merchant);
    }

    @Transactional(readOnly = true)
    public boolean existsByDocument(String document) {
        return merchantRepository.existsByDocument(document);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return merchantRepository.existsByEmail(email);
    }

    private void validateMerchant(Merchant merchant) {
        if (merchant.getDocument() == null || merchant.getDocument().isBlank()) {
            throw new BusinessException("DOCUMENT_REQUIRED", "Document (CNPJ/CPF) is required");
        }
        if (merchant.getName() == null || merchant.getName().isBlank()) {
            throw new BusinessException("NAME_REQUIRED", "Name is required");
        }
        if (existsByDocument(merchant.getDocument())) {
            throw new BusinessException("DUPLICATE_DOCUMENT", "Merchant with this document already exists");
        }
        if (merchant.getEmail() != null && existsByEmail(merchant.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Merchant with this email already exists");
        }
    }
}
