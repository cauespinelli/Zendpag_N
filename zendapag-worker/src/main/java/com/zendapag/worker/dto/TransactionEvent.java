package com.zendapag.worker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String transactionId;
    private String endToEndId;
    private String type;
    private BigDecimal amount;
    private String description;
    private String sourceAccountId;
    private String targetAccountId;
    private String pixKey;
    private String status;
    private LocalDateTime timestamp;
    private String metadata;
}