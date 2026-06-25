package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Razão de transações (extrato). Admin only. Lista os lançamentos do razão
 * (PAYMENT, FEE, REFUND, SETTLEMENT, WITHDRAWAL...) gerados pelo motor.
 */
@Tag(name = "Transactions", description = "Razão financeiro / extrato")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionRepository transactionRepository;

    @Operation(summary = "List all ledger transactions (admin)",
        description = "Lista paginada de todos os lançamentos do razão.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 500) size = 100;
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Map<String, Object>> result = transactionRepository.findAll(pr).map(this::summary);
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", result));
    }

    private Map<String, Object> summary(Transaction t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId().toString());
        m.put("referenceId", t.getReferenceId());
        m.put("type", t.getType() != null ? t.getType().name() : null);
        m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
        m.put("amount", t.getAmount());
        m.put("feeAmount", t.getFeeAmount());
        m.put("netAmount", t.getNetAmount());
        m.put("merchantName", t.getMerchant() != null ? t.getMerchant().getName() : null);
        m.put("description", t.getDescription());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }
}
