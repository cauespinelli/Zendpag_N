package com.zendapag.worker.services;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Transaction;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class FeesService {

    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.0199"); // 1.99%
    private static final BigDecimal MIN_FEE = new BigDecimal("0.50");

    public BigDecimal calculateTransactionFee(Merchant merchant, Transaction transaction) {
        log.debug("Calculating fee for transaction: {} merchant: {}",
            transaction.getId(), merchant.getId());

        BigDecimal amount = transaction.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Get fee rate from merchant config or use default
        BigDecimal feeRate = getMerchantFeeRate(merchant);
        BigDecimal calculatedFee = amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);

        // Apply minimum fee
        if (calculatedFee.compareTo(MIN_FEE) < 0) {
            calculatedFee = MIN_FEE;
        }

        log.debug("Fee calculated: {} for amount: {}", calculatedFee, amount);
        return calculatedFee;
    }

    public BigDecimal getMerchantFeeRate(Merchant merchant) {
        // TODO: Get fee rate from merchant configuration
        return DEFAULT_FEE_RATE;
    }

    public BigDecimal calculateSettlementFee(Merchant merchant, BigDecimal settlementAmount) {
        // Fixed settlement fee
        return new BigDecimal("2.00");
    }
}
