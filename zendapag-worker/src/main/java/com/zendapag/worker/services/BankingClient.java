package com.zendapag.worker.services;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class BankingClient {

    public PayoutResponse createPayout(PayoutRequest request) {
        log.info("Creating payout for merchant: {} amount: {}",
            request.getMerchantId(), request.getAmount());

        // TODO: Implement actual banking integration
        String payoutId = "PAYOUT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return PayoutResponse.builder()
            .payoutId(payoutId)
            .status("PROCESSING")
            .merchantId(request.getMerchantId())
            .amount(request.getAmount())
            .build();
    }

    public PayoutStatus getPayoutStatus(String payoutId) {
        log.info("Getting payout status for: {}", payoutId);
        // TODO: Implement actual status check
        return PayoutStatus.COMPLETED;
    }

    @Data
    @Builder
    public static class PayoutRequest {
        private String merchantId;
        private BigDecimal amount;
        private String currency;
        private String description;
        private String externalReference;
        private String bankAccount;
    }

    @Data
    @Builder
    public static class PayoutResponse {
        private String payoutId;
        private String status;
        private String merchantId;
        private BigDecimal amount;
        private String errorMessage;
    }

    public enum PayoutStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
}
