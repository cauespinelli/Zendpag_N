package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.Settlement;
import com.zendapag.core.repository.SettlementRepository;
import com.zendapag.core.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Liquidação (repasse). Admin only. Em sandbox a liquidação é disparada
 * manualmente (sem agendador/PSP).
 */
@Tag(name = "Settlements", description = "Settlement / repasse API")
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {

    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;

    @Operation(summary = "Run settlement for a merchant (admin/sandbox)",
        description = "Consolida os pagamentos aprovados e não liquidados do estabelecimento num lote de repasse.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/run/{merchantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> run(@PathVariable UUID merchantId) {
        log.info("Running settlement for merchant: {}", merchantId);
        Settlement settlement = settlementService.settleMerchant(merchantId);
        return ResponseEntity.ok(ApiResponse.success("Settlement criada e liquidada", summary(settlement)));
    }

    @Operation(summary = "List settlements (admin)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        List<Map<String, Object>> all = settlementRepository.findAll().stream().map(this::summary).toList();
        return ResponseEntity.ok(ApiResponse.success("Settlements", all));
    }

    private Map<String, Object> summary(Settlement s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId().toString());
        m.put("referenceId", s.getReferenceId());
        m.put("status", s.getStatus().name());
        m.put("paymentCount", s.getPaymentCount());
        m.put("transactionCount", s.getTransactionCount());
        m.put("grossAmount", s.getGrossAmount());
        m.put("feeAmount", s.getFeeAmount());
        m.put("netAmount", s.getNetAmount());
        m.put("settledAt", s.getSettledAt());
        return m;
    }
}
