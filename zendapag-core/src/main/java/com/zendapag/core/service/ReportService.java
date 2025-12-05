package com.zendapag.core.service;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Settlement;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.entity.enums.SettlementStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.exception.BusinessException;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.repository.SettlementRepository;
import com.zendapag.core.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ReportService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ReportService(PaymentRepository paymentRepository,
                        TransactionRepository transactionRepository,
                        SettlementRepository settlementRepository,
                        AuditService auditService,
                        ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.settlementRepository = settlementRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Timed(value = "report.financial_summary")
    @Cacheable(value = "reports", key = "#merchant.id + '_' + #startDate + '_' + #endDate")
    public FinancialSummaryReport generateFinancialSummary(Merchant merchant, LocalDate startDate, LocalDate endDate) {
        log.info("Generating financial summary report for merchant: {} period: {} to {}",
            merchant.getDocument(), startDate, endDate);

        try {
            Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            PaymentStats paymentStats = calculatePaymentStats(merchant, startInstant, endInstant);
            TransactionStats transactionStats = calculateTransactionStats(merchant, startInstant, endInstant);
            SettlementStats settlementStats = calculateSettlementStats(merchant, startInstant, endInstant);
            RevenueStats revenueStats = calculateRevenueStats(paymentStats, transactionStats);

            FinancialSummaryReport report = new FinancialSummaryReport(
                merchant.getId().toString(), merchant.getDocument(), startDate, endDate,
                paymentStats, transactionStats, settlementStats, revenueStats, Instant.now()
            );

            auditService.logAction(merchant, "Report", "financial_summary",
                AuditAction.VIEW, "Financial summary report generated for period: " + startDate + " to " + endDate);

            return report;
        } catch (Exception e) {
            log.error("Error generating financial summary report: {}", e.getMessage(), e);
            throw new BusinessException("Failed to generate financial summary report: " + e.getMessage());
        }
    }

    @Timed(value = "report.payment_details")
    public PaymentDetailsReport generatePaymentDetailsReport(Merchant merchant, LocalDate startDate, LocalDate endDate,
                                                            PaymentStatus status, int page, int size) {
        Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Payment> payments = paymentRepository.findByMerchantAndCreatedAtBetween(merchant, startInstant, endInstant);

        List<PaymentDetail> paymentDetails = payments.stream()
            .skip((long) page * size)
            .limit(size)
            .map(this::convertToPaymentDetail)
            .collect(Collectors.toList());

        return new PaymentDetailsReport(merchant.getId().toString(), merchant.getDocument(),
            startDate, endDate, status, paymentDetails, payments.size(), page, size, Instant.now());
    }

    @Timed(value = "report.settlement_history")
    public SettlementHistoryReport generateSettlementHistoryReport(Merchant merchant, LocalDate startDate, LocalDate endDate) {
        List<Settlement> settlements = settlementRepository.findByMerchantAndSettlementDateBetween(merchant, startDate, endDate);

        List<SettlementDetail> details = settlements.stream()
            .map(this::convertToSettlementDetail)
            .collect(Collectors.toList());

        BigDecimal totalGross = settlements.stream().map(Settlement::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = settlements.stream().map(Settlement::getFeeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = settlements.stream().map(Settlement::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SettlementHistoryReport(merchant.getId().toString(), merchant.getDocument(),
            startDate, endDate, details, settlements.size(), totalGross, totalFees, totalNet, Instant.now());
    }

    @Timed(value = "report.transaction_ledger")
    public TransactionLedgerReport generateTransactionLedger(Merchant merchant, LocalDate startDate, LocalDate endDate,
                                                           TransactionType type, int page, int size) {
        Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Transaction> transactions = transactionRepository.findByMerchantAndCreatedAtBetween(merchant, startInstant, endInstant);

        List<TransactionDetail> details = transactions.stream()
            .skip((long) page * size)
            .limit(size)
            .map(this::convertToTransactionDetail)
            .collect(Collectors.toList());

        return new TransactionLedgerReport(merchant.getId().toString(), merchant.getDocument(),
            startDate, endDate, type, details, transactions.size(), page, size, Instant.now());
    }

    @Async
    public CompletableFuture<byte[]> exportReportToCsv(Object reportData, String reportType) {
        try {
            return CompletableFuture.completedFuture(("ReportType," + reportType).getBytes());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<byte[]> exportReportToJson(Object reportData) {
        try {
            return CompletableFuture.completedFuture(objectMapper.writeValueAsString(reportData).getBytes());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private PaymentStats calculatePaymentStats(Merchant merchant, Instant startDate, Instant endDate) {
        Map<PaymentStatus, Long> statusCounts = new HashMap<>();
        Map<PaymentStatus, BigDecimal> statusAmounts = new HashMap<>();

        for (PaymentStatus status : PaymentStatus.values()) {
            Long count = paymentRepository.countByMerchantAndCreatedAtBetweenAndStatus(merchant, startDate, endDate, status);
            statusCounts.put(status, count != null ? count : 0L);
            BigDecimal amount = paymentRepository.sumAmountByMerchantAndCreatedAtBetweenAndStatus(merchant, startDate, endDate, status);
            statusAmounts.put(status, amount != null ? amount : BigDecimal.ZERO);
        }

        long totalPayments = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        BigDecimal totalAmount = statusAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageAmount = totalPayments > 0 ?
            totalAmount.divide(BigDecimal.valueOf(totalPayments), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return new PaymentStats(totalPayments, totalAmount, averageAmount, statusCounts, statusAmounts);
    }

    private TransactionStats calculateTransactionStats(Merchant merchant, Instant startDate, Instant endDate) {
        List<Transaction> transactions = transactionRepository.findByMerchantAndCreatedAtBetween(merchant, startDate, endDate);
        Map<TransactionType, Long> typeCounts = transactions.stream().collect(Collectors.groupingBy(Transaction::getType, Collectors.counting()));
        Map<TransactionType, BigDecimal> typeAmounts = transactions.stream().collect(Collectors.groupingBy(Transaction::getType,
            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        return new TransactionStats(transactions.size(),
            typeAmounts.getOrDefault(TransactionType.CREDIT, BigDecimal.ZERO),
            typeAmounts.getOrDefault(TransactionType.DEBIT, BigDecimal.ZERO), typeCounts, typeAmounts);
    }

    private SettlementStats calculateSettlementStats(Merchant merchant, Instant startDate, Instant endDate) {
        LocalDate localStart = startDate.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate localEnd = endDate.atZone(ZoneOffset.UTC).toLocalDate();
        List<Settlement> settlements = settlementRepository.findByMerchantAndSettlementDateBetween(merchant, localStart, localEnd);

        return new SettlementStats(settlements.size(),
            settlements.stream().map(Settlement::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
            settlements.stream().map(Settlement::getFeeAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
            settlements.stream().map(Settlement::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private RevenueStats calculateRevenueStats(PaymentStats paymentStats, TransactionStats transactionStats) {
        BigDecimal grossRevenue = paymentStats.statusAmounts().getOrDefault(PaymentStatus.APPROVED, BigDecimal.ZERO);
        BigDecimal fees = transactionStats.typeAmounts().getOrDefault(TransactionType.DEBIT, BigDecimal.ZERO);
        BigDecimal netRevenue = grossRevenue.subtract(fees);
        BigDecimal feeRate = grossRevenue.compareTo(BigDecimal.ZERO) > 0 ?
            fees.divide(grossRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        return new RevenueStats(grossRevenue, fees, netRevenue, feeRate);
    }

    private PaymentDetail convertToPaymentDetail(Payment p) {
        return new PaymentDetail(p.getId().toString(), p.getReferenceId(), p.getAmount(), p.getCurrency(),
            p.getStatus(), p.getDescription(), p.getCreatedAt(), p.getProcessedAt(), p.getCustomerEmail());
    }

    private SettlementDetail convertToSettlementDetail(Settlement s) {
        return new SettlementDetail(s.getId().toString(), s.getReferenceId(), s.getGrossAmount(), s.getFeeAmount(),
            s.getNetAmount(), s.getTransactionCount(), s.getStatus(), s.getSettlementDate(), s.getCompletedAt());
    }

    private TransactionDetail convertToTransactionDetail(Transaction t) {
        return new TransactionDetail(t.getId().toString(), t.getReferenceId(), t.getType(), t.getAmount(),
            t.getDescription(), t.getBalanceBefore(), t.getBalanceAfter(), t.getCreatedAt());
    }

    // Report DTOs using Java records
    public record FinancialSummaryReport(String merchantId, String merchantDocument, LocalDate startDate, LocalDate endDate,
        PaymentStats paymentStats, TransactionStats transactionStats, SettlementStats settlementStats, RevenueStats revenueStats, Instant generatedAt) {}
    public record PaymentDetailsReport(String merchantId, String merchantDocument, LocalDate startDate, LocalDate endDate,
        PaymentStatus status, List<PaymentDetail> payments, long totalCount, int page, int size, Instant generatedAt) {}
    public record SettlementHistoryReport(String merchantId, String merchantDocument, LocalDate startDate, LocalDate endDate,
        List<SettlementDetail> settlements, int totalSettlements, BigDecimal totalGross, BigDecimal totalFees, BigDecimal totalNet, Instant generatedAt) {}
    public record TransactionLedgerReport(String merchantId, String merchantDocument, LocalDate startDate, LocalDate endDate,
        TransactionType type, List<TransactionDetail> transactions, long totalCount, int page, int size, Instant generatedAt) {}
    public record PaymentStats(long totalPayments, BigDecimal totalAmount, BigDecimal averageAmount,
        Map<PaymentStatus, Long> statusCounts, Map<PaymentStatus, BigDecimal> statusAmounts) {}
    public record TransactionStats(long totalTransactions, BigDecimal totalCredits, BigDecimal totalDebits,
        Map<TransactionType, Long> typeCounts, Map<TransactionType, BigDecimal> typeAmounts) {}
    public record SettlementStats(long totalSettlements, BigDecimal totalGross, BigDecimal totalFees, BigDecimal totalNet) {}
    public record RevenueStats(BigDecimal grossRevenue, BigDecimal fees, BigDecimal netRevenue, BigDecimal feeRate) {}
    public record PaymentDetail(String id, String referenceId, BigDecimal amount, String currency,
        PaymentStatus status, String description, Instant createdAt, Instant processedAt, String customerEmail) {}
    public record SettlementDetail(String id, String referenceId, BigDecimal grossAmount, BigDecimal feeAmount,
        BigDecimal netAmount, int transactionCount, SettlementStatus status, LocalDate settlementDate, Instant completedAt) {}
    public record TransactionDetail(String id, String referenceId, TransactionType type, BigDecimal amount,
        String description, BigDecimal balanceBefore, BigDecimal balanceAfter, Instant createdAt) {}
}
