package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.service.MerchantService;
import com.zendapag.core.service.ReportService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Tag(name = "Reports", description = "Financial reports and analytics API")
@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasRole('MERCHANT')")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final MerchantService merchantService;

    @Operation(
        summary = "Get financial summary",
        description = "Returns a comprehensive financial summary report for the specified date range.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Financial summary retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/financial-summary")
    @RateLimiter(name = "reports-api")
    @Timed(value = "api.reports.financial.summary", description = "Time taken to generate financial summary")
    public ResponseEntity<ApiResponse<ReportService.FinancialSummaryReport>> getFinancialSummary(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        log.info("Generating financial summary for merchant: {} period: {} to {}",
            authentication.getName(), startDate, endDate);

        validateDateRange(startDate, endDate);
        Merchant merchant = getMerchantFromAuth(authentication);

        ReportService.FinancialSummaryReport report = reportService.generateFinancialSummary(
            merchant, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success("Financial summary generated", report));
    }

    @Operation(
        summary = "Get payment details report",
        description = "Returns detailed payment information with pagination and filtering options.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/payments")
    @RateLimiter(name = "reports-api")
    @Timed(value = "api.reports.payments", description = "Time taken to generate payment details report")
    public ResponseEntity<ApiResponse<ReportService.PaymentDetailsReport>> getPaymentDetailsReport(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filter by payment status")
            @RequestParam(required = false) PaymentStatus status,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        validateDateRange(startDate, endDate);
        Merchant merchant = getMerchantFromAuth(authentication);

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size <= 0 || size > 1000) size = 50;

        ReportService.PaymentDetailsReport report = reportService.generatePaymentDetailsReport(
            merchant, startDate, endDate, status, page, size);

        return ResponseEntity.ok(ApiResponse.success("Payment details report generated", report));
    }

    @Operation(
        summary = "Get settlement history",
        description = "Returns settlement history report for the specified date range.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/settlements")
    @RateLimiter(name = "reports-api")
    @Timed(value = "api.reports.settlements", description = "Time taken to generate settlement history")
    public ResponseEntity<ApiResponse<ReportService.SettlementHistoryReport>> getSettlementHistory(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        validateDateRange(startDate, endDate);
        Merchant merchant = getMerchantFromAuth(authentication);

        ReportService.SettlementHistoryReport report = reportService.generateSettlementHistoryReport(
            merchant, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success("Settlement history report generated", report));
    }

    @Operation(
        summary = "Get transaction ledger",
        description = "Returns transaction ledger report with detailed transaction history.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/transactions")
    @RateLimiter(name = "reports-api")
    @Timed(value = "api.reports.transactions", description = "Time taken to generate transaction ledger")
    public ResponseEntity<ApiResponse<ReportService.TransactionLedgerReport>> getTransactionLedger(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filter by transaction type")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        validateDateRange(startDate, endDate);
        Merchant merchant = getMerchantFromAuth(authentication);

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size <= 0 || size > 1000) size = 50;

        ReportService.TransactionLedgerReport report = reportService.generateTransactionLedger(
            merchant, startDate, endDate, type, page, size);

        return ResponseEntity.ok(ApiResponse.success("Transaction ledger report generated", report));
    }

    @Operation(
        summary = "Export financial summary to CSV",
        description = "Exports financial summary report to CSV format for download.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/financial-summary/export/csv")
    @RateLimiter(name = "reports-api")
    @Timed(value = "api.reports.export.csv", description = "Time taken to export CSV")
    public CompletableFuture<ResponseEntity<byte[]>> exportFinancialSummaryCsv(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        log.info("Exporting financial summary CSV for merchant: {} period: {} to {}",
            authentication.getName(), startDate, endDate);

        validateDateRange(startDate, endDate);
        Merchant merchant = getMerchantFromAuth(authentication);

        ReportService.FinancialSummaryReport report = reportService.generateFinancialSummary(
            merchant, startDate, endDate);

        return reportService.exportReportToCsv(report, "financial_summary")
            .thenApply(csvBytes -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.setContentDispositionFormData("attachment",
                    String.format("financial-summary-%s-%s.csv", startDate, endDate));
                headers.setContentLength(csvBytes.length);

                return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
            })
            .exceptionally(throwable -> {
                log.error("Error exporting CSV: {}", throwable.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            });
    }

    @Operation(
        summary = "Export payments to CSV",
        description = "Exports payment details report to CSV format for download.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/payments/export/csv")
    @RateLimiter(name = "reports-api")
    @Timed(value = "api.reports.payments.export.csv", description = "Time taken to export payments CSV")
    public CompletableFuture<ResponseEntity<byte[]>> exportPaymentsCsv(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filter by payment status")
            @RequestParam(required = false) PaymentStatus status,
            Authentication authentication) {

        validateDateRange(startDate, endDate);
        Merchant merchant = getMerchantFromAuth(authentication);

        // Export all pages for CSV
        ReportService.PaymentDetailsReport report = reportService.generatePaymentDetailsReport(
            merchant, startDate, endDate, status, 0, 10000);

        return reportService.exportReportToCsv(report, "payment_details")
            .thenApply(csvBytes -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.setContentDispositionFormData("attachment",
                    String.format("payments-%s-%s.csv", startDate, endDate));
                headers.setContentLength(csvBytes.length);

                return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
            })
            .exceptionally(throwable -> {
                log.error("Error exporting payments CSV: {}", throwable.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            });
    }

    @Operation(
        summary = "Export settlements to CSV",
        description = "Exports settlement history report to CSV format for download.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/settlements/export/csv")
    @RateLimiter(name = "reports-api")
    @Timed(value = "api.reports.settlements.export.csv", description = "Time taken to export settlements CSV")
    public CompletableFuture<ResponseEntity<byte[]>> exportSettlementsCsv(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        validateDateRange(startDate, endDate);
        Merchant merchant = getMerchantFromAuth(authentication);

        ReportService.SettlementHistoryReport report = reportService.generateSettlementHistoryReport(
            merchant, startDate, endDate);

        return reportService.exportReportToCsv(report, "settlement_history")
            .thenApply(csvBytes -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.setContentDispositionFormData("attachment",
                    String.format("settlements-%s-%s.csv", startDate, endDate));
                headers.setContentLength(csvBytes.length);

                return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
            })
            .exceptionally(throwable -> {
                log.error("Error exporting settlements CSV: {}", throwable.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            });
    }

    @Operation(
        summary = "Export transactions to CSV",
        description = "Exports transaction ledger report to CSV format for download.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/transactions/export/csv")
    @RateLimiter(name = "reports-api")
    @Timed(value = "api.reports.transactions.export.csv", description = "Time taken to export transactions CSV")
    public CompletableFuture<ResponseEntity<byte[]>> exportTransactionsCsv(
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filter by transaction type")
            @RequestParam(required = false) TransactionType type,
            Authentication authentication) {

        validateDateRange(startDate, endDate);
        Merchant merchant = getMerchantFromAuth(authentication);

        // Export all pages for CSV
        ReportService.TransactionLedgerReport report = reportService.generateTransactionLedger(
            merchant, startDate, endDate, type, 0, 10000);

        return reportService.exportReportToCsv(report, "transaction_ledger")
            .thenApply(csvBytes -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.setContentDispositionFormData("attachment",
                    String.format("transactions-%s-%s.csv", startDate, endDate));
                headers.setContentLength(csvBytes.length);

                return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
            })
            .exceptionally(throwable -> {
                log.error("Error exporting transactions CSV: {}", throwable.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            });
    }

    @Operation(
        summary = "Export report to JSON",
        description = "Exports any report to JSON format for programmatic consumption.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{reportType}/export/json")
    @RateLimiter(name = "reports-api")
    @Timed(value = "api.reports.export.json", description = "Time taken to export JSON")
    public CompletableFuture<ResponseEntity<byte[]>> exportReportJson(
            @Parameter(description = "Report type", example = "financial-summary")
            @PathVariable String reportType,
            @Parameter(description = "Start date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Additional parameters (status, type, etc.)")
            @RequestParam(required = false) String filter,
            Authentication authentication) {

        validateDateRange(startDate, endDate);
        Merchant merchant = getMerchantFromAuth(authentication);

        Object report = generateReportByType(merchant, reportType, startDate, endDate, filter);

        return reportService.exportReportToJson(report)
            .thenApply(jsonBytes -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setContentDispositionFormData("attachment",
                    String.format("%s-%s-%s.json", reportType, startDate, endDate));
                headers.setContentLength(jsonBytes.length);

                return new ResponseEntity<>(jsonBytes, headers, HttpStatus.OK);
            })
            .exceptionally(throwable -> {
                log.error("Error exporting JSON: {}", throwable.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            });
    }

    // Helper methods

    private Merchant getMerchantFromAuth(Authentication authentication) {
        String merchantDocument = authentication.getName();
        return merchantService.findByDocument(merchantDocument);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("Start date and end date are required", "INVALID_DATE_RANGE");
        }

        if (startDate.isAfter(endDate)) {
            throw new BusinessException("Start date must be before end date", "INVALID_DATE_RANGE");
        }

        if (startDate.isBefore(LocalDate.now().minusYears(2))) {
            throw new BusinessException("Start date cannot be more than 2 years ago", "INVALID_DATE_RANGE");
        }

        if (endDate.isAfter(LocalDate.now())) {
            throw new BusinessException("End date cannot be in the future", "INVALID_DATE_RANGE");
        }

        // Maximum 1 year range
        if (startDate.isBefore(endDate.minusYears(1))) {
            throw new BusinessException("Date range cannot exceed 1 year", "INVALID_DATE_RANGE");
        }
    }

    private Object generateReportByType(Merchant merchant, String reportType, LocalDate startDate,
                                       LocalDate endDate, String filter) {
        switch (reportType.toLowerCase()) {
            case "financial-summary":
                return reportService.generateFinancialSummary(merchant, startDate, endDate);
            case "payments":
                PaymentStatus status = filter != null ? PaymentStatus.valueOf(filter.toUpperCase()) : null;
                return reportService.generatePaymentDetailsReport(merchant, startDate, endDate, status, 0, 10000);
            case "settlements":
                return reportService.generateSettlementHistoryReport(merchant, startDate, endDate);
            case "transactions":
                TransactionType type = filter != null ? TransactionType.valueOf(filter.toUpperCase()) : null;
                return reportService.generateTransactionLedger(merchant, startDate, endDate, type, 0, 10000);
            default:
                throw new BusinessException("Unsupported report type: " + reportType, "UNSUPPORTED_REPORT_TYPE");
        }
    }
}