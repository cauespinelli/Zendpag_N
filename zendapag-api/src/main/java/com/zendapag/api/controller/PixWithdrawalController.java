package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.dto.request.CreatePixWithdrawalRequest;
import com.zendapag.core.dto.response.PixWithdrawalResponse;
import com.zendapag.core.entity.enums.WithdrawalStatus;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.service.PixWithdrawalService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller para gerenciamento de saques PIX
 */
@Tag(name = "PIX Withdrawals", description = "PIX withdrawal management API")
@RestController
@RequestMapping("/api/v1/withdrawals")
@PreAuthorize("hasRole('MERCHANT') or hasRole('USER')")
@Validated
@RequiredArgsConstructor
@Slf4j
public class PixWithdrawalController {

    private final PixWithdrawalService withdrawalService;

    @Operation(
        summary = "Create PIX withdrawal",
        description = "Creates a new PIX withdrawal request. Validates balance and limits before processing."
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "201", description = "Withdrawal created successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Invalid request data or insufficient balance"),
        @SwaggerApiResponse(responseCode = "401", description = "Authentication required"),
        @SwaggerApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @SwaggerApiResponse(responseCode = "429", description = "Rate limit exceeded or too many pending withdrawals"),
        @SwaggerApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @RateLimiter(name = "withdrawal-api")
    @Timed(name = "api.withdrawals.create", description = "Time taken to create PIX withdrawal")
    public ResponseEntity<ApiResponse<PixWithdrawalResponse>> createWithdrawal(
            @Valid @RequestBody CreatePixWithdrawalRequest request,
            @RequestParam @Parameter(description = "Account ID") UUID accountId,
            @RequestParam @Parameter(description = "Merchant ID") UUID merchantId,
            Authentication authentication) {

        log.info("Creating PIX withdrawal for account: {}, amount: {}",
            accountId, request.getAmount());

        try {
            PixWithdrawalResponse response = withdrawalService.createWithdrawal(accountId, merchantId, request);

            log.info("PIX withdrawal created successfully: {}", response.getReferenceId());

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PIX withdrawal created successfully", response));

        } catch (BusinessException e) {
            log.warn("Business error creating withdrawal: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating withdrawal: {}", e.getMessage(), e);
            throw new BusinessException("Failed to create withdrawal", e);
        }
    }

    @Operation(
        summary = "Get withdrawal by ID",
        description = "Retrieves withdrawal details by withdrawal ID"
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Withdrawal found"),
        @SwaggerApiResponse(responseCode = "404", description = "Withdrawal not found"),
        @SwaggerApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @Timed(name = "api.withdrawals.get", description = "Time taken to get withdrawal by ID")
    public ResponseEntity<ApiResponse<PixWithdrawalResponse>> getWithdrawalById(
            @PathVariable @Parameter(description = "Withdrawal ID") UUID id) {

        log.debug("Fetching withdrawal: {}", id);

        return withdrawalService.findById(id)
            .map(withdrawal -> {
                PixWithdrawalResponse response = withdrawalService.convertToResponse(withdrawal);
                return ResponseEntity.ok(ApiResponse.success("Withdrawal found", response));
            })
            .orElseThrow(() -> new BusinessException("Withdrawal not found: " + id));
    }

    @Operation(
        summary = "Get withdrawal by reference ID",
        description = "Retrieves withdrawal details by reference ID"
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Withdrawal found"),
        @SwaggerApiResponse(responseCode = "404", description = "Withdrawal not found"),
        @SwaggerApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/reference/{referenceId}")
    @Timed(name = "api.withdrawals.get.by.reference", description = "Time taken to get withdrawal by reference")
    public ResponseEntity<ApiResponse<PixWithdrawalResponse>> getWithdrawalByReference(
            @PathVariable @Parameter(description = "Reference ID") String referenceId) {

        log.debug("Fetching withdrawal by reference: {}", referenceId);

        return withdrawalService.findByReferenceId(referenceId)
            .map(withdrawal -> {
                PixWithdrawalResponse response = withdrawalService.convertToResponse(withdrawal);
                return ResponseEntity.ok(ApiResponse.success("Withdrawal found", response));
            })
            .orElseThrow(() -> new BusinessException("Withdrawal not found: " + referenceId));
    }

    @Operation(
        summary = "List withdrawals by account",
        description = "Lists all withdrawals for a specific account with pagination"
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Withdrawals retrieved successfully"),
        @SwaggerApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/account/{accountId}")
    @Timed(name = "api.withdrawals.list.by.account", description = "Time taken to list withdrawals by account")
    public ResponseEntity<ApiResponse<Page<PixWithdrawalResponse>>> listWithdrawalsByAccount(
            @PathVariable @Parameter(description = "Account ID") UUID accountId,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size,
            @RequestParam(defaultValue = "createdAt") @Parameter(description = "Sort field") String sortBy,
            @RequestParam(defaultValue = "DESC") @Parameter(description = "Sort direction") String sortDir) {

        log.debug("Listing withdrawals for account: {}, page: {}, size: {}", accountId, page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<PixWithdrawalResponse> withdrawals = withdrawalService.findByAccount(accountId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("Withdrawals retrieved successfully", withdrawals));
    }

    @Operation(
        summary = "List withdrawals by merchant",
        description = "Lists all withdrawals for a specific merchant with pagination"
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Withdrawals retrieved successfully"),
        @SwaggerApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/merchant/{merchantId}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Timed(name = "api.withdrawals.list.by.merchant", description = "Time taken to list withdrawals by merchant")
    public ResponseEntity<ApiResponse<Page<PixWithdrawalResponse>>> listWithdrawalsByMerchant(
            @PathVariable @Parameter(description = "Merchant ID") UUID merchantId,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size,
            @RequestParam(defaultValue = "createdAt") @Parameter(description = "Sort field") String sortBy,
            @RequestParam(defaultValue = "DESC") @Parameter(description = "Sort direction") String sortDir) {

        log.debug("Listing withdrawals for merchant: {}, page: {}, size: {}", merchantId, page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<PixWithdrawalResponse> withdrawals = withdrawalService.findByMerchant(merchantId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("Withdrawals retrieved successfully", withdrawals));
    }

    @Operation(
        summary = "List withdrawals by status",
        description = "Lists all withdrawals with a specific status with pagination"
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Withdrawals retrieved successfully"),
        @SwaggerApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(name = "api.withdrawals.list.by.status", description = "Time taken to list withdrawals by status")
    public ResponseEntity<ApiResponse<Page<PixWithdrawalResponse>>> listWithdrawalsByStatus(
            @PathVariable @Parameter(description = "Withdrawal status") WithdrawalStatus status,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {

        log.debug("Listing withdrawals with status: {}, page: {}, size: {}", status, page, size);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<PixWithdrawalResponse> withdrawals = withdrawalService.findByStatus(status, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("Withdrawals retrieved successfully", withdrawals));
    }

    @Operation(
        summary = "Cancel withdrawal",
        description = "Cancels a pending or processing withdrawal"
    )
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "Withdrawal cancelled successfully"),
        @SwaggerApiResponse(responseCode = "400", description = "Withdrawal cannot be cancelled"),
        @SwaggerApiResponse(responseCode = "404", description = "Withdrawal not found"),
        @SwaggerApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/cancel")
    @Timed(name = "api.withdrawals.cancel", description = "Time taken to cancel withdrawal")
    public ResponseEntity<ApiResponse<PixWithdrawalResponse>> cancelWithdrawal(
            @PathVariable @Parameter(description = "Withdrawal ID") UUID id,
            @RequestParam @Parameter(description = "Cancellation reason") String reason) {

        log.info("Cancelling withdrawal: {}, reason: {}", id, reason);

        try {
            PixWithdrawalResponse response = withdrawalService.cancelWithdrawal(id, reason);

            log.info("Withdrawal cancelled successfully: {}", id);

            return ResponseEntity.ok(ApiResponse.success("Withdrawal cancelled successfully", response));

        } catch (BusinessException e) {
            log.warn("Business error cancelling withdrawal: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error cancelling withdrawal: {}", e.getMessage(), e);
            throw new BusinessException("Failed to cancel withdrawal", e);
        }
    }
}
