package com.zendapag.core.dto.response;

import com.zendapag.core.entity.enums.WithdrawalStatus;
import com.zendapag.core.entity.enums.WithdrawalTriggerType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO para resposta de saque PIX
 */
public class PixWithdrawalResponse {

    private UUID id;
    private String referenceId;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private String pixKey;
    private String pixKeyType;
    private String recipientName;
    private String recipientDocument;
    private String recipientBank;
    private WithdrawalStatus status;
    private WithdrawalTriggerType triggerType;
    private String description;
    private String rejectionReason;
    private String pixTransactionId;
    private String pixEndToEndId;
    private Instant requestedAt;
    private Instant processedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant expiresAt;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public PixWithdrawalResponse() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getPixKey() {
        return pixKey;
    }

    public void setPixKey(String pixKey) {
        this.pixKey = pixKey;
    }

    public String getPixKeyType() {
        return pixKeyType;
    }

    public void setPixKeyType(String pixKeyType) {
        this.pixKeyType = pixKeyType;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientDocument() {
        return recipientDocument;
    }

    public void setRecipientDocument(String recipientDocument) {
        this.recipientDocument = recipientDocument;
    }

    public String getRecipientBank() {
        return recipientBank;
    }

    public void setRecipientBank(String recipientBank) {
        this.recipientBank = recipientBank;
    }

    public WithdrawalStatus getStatus() {
        return status;
    }

    public void setStatus(WithdrawalStatus status) {
        this.status = status;
    }

    public WithdrawalTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(WithdrawalTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getPixTransactionId() {
        return pixTransactionId;
    }

    public void setPixTransactionId(String pixTransactionId) {
        this.pixTransactionId = pixTransactionId;
    }

    public String getPixEndToEndId() {
        return pixEndToEndId;
    }

    public void setPixEndToEndId(String pixEndToEndId) {
        this.pixEndToEndId = pixEndToEndId;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
