package com.zendapag.core.events;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event published when a settlement batch is completed.
 * Contains information about processed payments and transfer details.
 */
public class SettlementCompletedEvent extends DomainEvent {

    @NotNull
    private final String settlementId;

    @NotNull
    private final String merchantId;

    @NotNull
    private final String batchId;

    @NotNull
    @Positive
    private final BigDecimal totalAmount;

    @NotNull
    @Positive
    private final BigDecimal netAmount;

    @NotNull
    private final BigDecimal feesAmount;

    @NotNull
    private final String currency;

    @NotNull
    private final Instant settlementDate;

    @NotNull
    private final Instant completedAt;

    @NotNull
    private final String bankAccount;

    private final String transferId;
    private final String bankTransactionId;
    private final List<String> paymentIds;
    private final Integer paymentCount;
    private final String settlementMethod;
    private final Map<String, Object> bankDetails;
    private final Map<String, BigDecimal> feeBreakdown;
    private final String reconciliationRef;

    public SettlementCompletedEvent(String settlementId,
                                  String merchantId,
                                  String batchId,
                                  BigDecimal totalAmount,
                                  BigDecimal netAmount,
                                  BigDecimal feesAmount,
                                  String currency,
                                  Instant settlementDate,
                                  Instant completedAt,
                                  String bankAccount,
                                  String transferId,
                                  String bankTransactionId,
                                  List<String> paymentIds,
                                  Integer paymentCount,
                                  String settlementMethod,
                                  Map<String, Object> bankDetails,
                                  Map<String, BigDecimal> feeBreakdown,
                                  String reconciliationRef,
                                  String correlationId,
                                  String causationId) {

        super("settlement_completed", settlementId, "Settlement", correlationId, causationId, null);

        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.batchId = batchId;
        this.totalAmount = totalAmount;
        this.netAmount = netAmount;
        this.feesAmount = feesAmount;
        this.currency = currency;
        this.settlementDate = settlementDate;
        this.completedAt = completedAt;
        this.bankAccount = bankAccount;
        this.transferId = transferId;
        this.bankTransactionId = bankTransactionId;
        this.paymentIds = paymentIds != null ? List.copyOf(paymentIds) : List.of();
        this.paymentCount = paymentCount != null ? paymentCount : this.paymentIds.size();
        this.settlementMethod = settlementMethod;
        this.bankDetails = bankDetails != null ? Map.copyOf(bankDetails) : Map.of();
        this.feeBreakdown = feeBreakdown != null ? Map.copyOf(feeBreakdown) : Map.of();
        this.reconciliationRef = reconciliationRef;

        // Add contextual metadata
        this.withMetadata("merchant_id", merchantId)
            .withMetadata("settlement_method", settlementMethod)
            .withMetadata("total_amount", totalAmount.toString())
            .withMetadata("net_amount", netAmount.toString())
            .withMetadata("fees_amount", feesAmount.toString())
            .withMetadata("currency", currency)
            .withMetadata("payment_count", this.paymentCount)
            .withMetadata("settlement_date", settlementDate.toString())
            .withMetadata("has_bank_confirmation", hasBankConfirmation())
            .withMetadata("processing_time_ms", getProcessingTimeMillis());
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("settlementId", settlementId);
        data.put("merchantId", merchantId);
        data.put("batchId", batchId);
        data.put("totalAmount", totalAmount);
        data.put("netAmount", netAmount);
        data.put("feesAmount", feesAmount);
        data.put("currency", currency);
        data.put("settlementDate", settlementDate);
        data.put("completedAt", completedAt);
        data.put("bankAccount", bankAccount);
        data.put("transferId", transferId);
        data.put("bankTransactionId", bankTransactionId);
        data.put("paymentIds", paymentIds);
        data.put("paymentCount", paymentCount);
        data.put("settlementMethod", settlementMethod);
        data.put("bankDetails", bankDetails);
        data.put("feeBreakdown", feeBreakdown);
        data.put("reconciliationRef", reconciliationRef);
        return data;
    }

    // Getters
    public String getSettlementId() {
        return settlementId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getBatchId() {
        return batchId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public BigDecimal getFeesAmount() {
        return feesAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getSettlementDate() {
        return settlementDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getBankTransactionId() {
        return bankTransactionId;
    }

    public List<String> getPaymentIds() {
        return paymentIds;
    }

    public Integer getPaymentCount() {
        return paymentCount;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public Map<String, Object> getBankDetails() {
        return bankDetails;
    }

    public Map<String, BigDecimal> getFeeBreakdown() {
        return feeBreakdown;
    }

    public String getReconciliationRef() {
        return reconciliationRef;
    }

    // Business logic methods
    public boolean hasBankConfirmation() {
        return bankTransactionId != null && !bankTransactionId.isEmpty();
    }

    public boolean hasTransferId() {
        return transferId != null && !transferId.isEmpty();
    }

    public boolean isReconciled() {
        return reconciliationRef != null && !reconciliationRef.isEmpty();
    }

    public boolean includesPayment(String paymentId) {
        return paymentIds.contains(paymentId);
    }

    public BigDecimal getEffectiveFeeRate() {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return feesAmount.divide(totalAmount, 4, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getAveragePaymentAmount() {
        if (paymentCount == 0) {
            return BigDecimal.ZERO;
        }
        return totalAmount.divide(BigDecimal.valueOf(paymentCount), 2, BigDecimal.ROUND_HALF_UP);
    }

    public long getSettlementDelayMs() {
        return completedAt.toEpochMilli() - settlementDate.toEpochMilli();
    }

    public long getProcessingTimeMillis() {
        // This would ideally use the settlement initiation time
        // For now, we'll use the age of the event as a proxy
        return getAgeInMillis();
    }

    public BigDecimal getFeeByType(String feeType) {
        return feeBreakdown.getOrDefault(feeType, BigDecimal.ZERO);
    }

    public Object getBankDetail(String key) {
        return bankDetails.get(key);
    }

    public String getBankDetailAsString(String key) {
        Object value = getBankDetail(key);
        return value != null ? value.toString() : null;
    }

    // Validation methods
    public boolean hasRequiredFields() {
        return settlementId != null &&
               merchantId != null &&
               batchId != null &&
               totalAmount != null &&
               netAmount != null &&
               feesAmount != null &&
               currency != null &&
               settlementDate != null &&
               completedAt != null &&
               bankAccount != null;
    }

    public boolean hasValidAmounts() {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (feesAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        // Net amount should be total minus fees
        BigDecimal calculatedNet = totalAmount.subtract(feesAmount);
        return netAmount.compareTo(calculatedNet) == 0;
    }

    public boolean hasValidDates() {
        return settlementDate != null &&
               completedAt != null &&
               !completedAt.isBefore(settlementDate);
    }

    public boolean hasValidPaymentCount() {
        return paymentCount != null &&
               paymentCount > 0 &&
               (paymentIds.isEmpty() || paymentIds.size() == paymentCount);
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
               hasRequiredFields() &&
               hasValidAmounts() &&
               hasValidDates() &&
               hasValidPaymentCount();
    }

    // Utility methods
    public boolean isLargeSettlement() {
        // Consider settlements above 10,000 BRL as large
        return totalAmount.compareTo(new BigDecimal("10000")) > 0;
    }

    public boolean isHighFeeSettlement() {
        // Consider settlements with fees above 5% as high
        return getEffectiveFeeRate().compareTo(new BigDecimal("0.05")) > 0;
    }

    public boolean isDelayedSettlement() {
        // Consider settlements delayed by more than 1 hour as delayed
        return getSettlementDelayMs() > 3600000; // 1 hour in milliseconds
    }

    @Override
    public String getEventSummary() {
        return String.format("SettlementCompleted[id=%s, merchant=%s, amount=%s %s, payments=%d, method=%s]",
                settlementId, merchantId, netAmount, currency, paymentCount, settlementMethod);
    }
}