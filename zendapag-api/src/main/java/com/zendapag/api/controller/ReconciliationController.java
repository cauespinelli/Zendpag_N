package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.service.ReconciliationService;
import com.zendapag.core.service.ReconciliationService.ReconciliationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conciliação financeira. Admin only. Em sandbox roda manualmente (sem
 * agendador), comparando pagamentos aprovados x lançamentos do razão.
 */
@Tag(name = "Reconciliation", description = "Conciliação financeira")
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
@Slf4j
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @Operation(summary = "Run reconciliation (admin)",
        description = "Confere pagamentos APROVADOS contra os lançamentos do razão e retorna divergências.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReconciliationResult>> run() {
        log.info("Running reconciliation");
        ReconciliationResult result = reconciliationService.reconcileApprovedPayments();
        return ResponseEntity.ok(ApiResponse.success("Reconciliation completed", result));
    }
}
