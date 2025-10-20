package com.zendapag.core.service;

import com.zendapag.core.audit.AuditService;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Settlement;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.entity.enums.PaymentStatus;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
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

    @Timed
    @Cacheable
    public FinancialSummaryReport generateFinancialSummary {
        log.info("Generating financial summary report for merchant: {} period: {} to {}",
            merchant.getDocument, startDate, endDate);

        try {
            Instant startInstant = startDate.atStartOfDay.toInstant();
            Instant endInstant = endDate.plusDays.atStartOfDay(ZoneOffset.UTC).toInstant();

            // Payment statistics
            PaymentStats paymentStats = calculatePaymentStats;

            // Transaction statistics
            TransactionStats transactionStats = calculateTransactionStats;

            // Settlement statistics
            SettlementStats settlementStats = calculateSettlementStats;

            // Revenue calculations
            RevenueStats revenueStats = calculateRevenueStats;

            FinancialSummaryReport report = new FinancialSummaryReport(
                merchant.getId.toString(),
                merchant.getDocument,
                startDate,
                endDate,
                paymentStats,
                transactionStats,
                settlementStats,
                revenueStats,
                Instant.now
            );

            // Audit log
            auditService.logAction(merchant, "Report", "financial_summary",
                AuditAction.VIEW, "Financial summary report generated for period: " + startDate + " to " + endDate);

            log.info);
            return report;

        } catch  {
            log.error("Error generating financial summary report for merchant {}: {}",
                merchant.getDocument, e.getMessage(), e);
            auditService.logFailure.toString(),
                AuditAction.VIEW, "Financial summary report generation failed", e);
            throw new BusinessException;
        }
    }

    @Timed
    public PaymentDetailsReport generatePaymentDetailsReport(Merchant merchant, LocalDate startDate, LocalDate endDate,
                                                            PaymentStatus status, int page, int size) {
        log.info("Generating payment details report for merchant: {} period: {} to {} status: {}",
            merchant.getDocument, startDate, endDate, status);

        try {
            Instant startInstant = startDate.atStartOfDay.toInstant();
            Instant endInstant = endDate.plusDays.atStartOfDay(ZoneOffset.UTC).toInstant();

            List<Payment> payments;
            long totalCount;

            if  {
                payments = paymentRepository.findByMerchantAndCreatedAtBetweenAndStatus(
                    merchant, startInstant, endInstant, status,
                    org.springframework.data.domain.PageRequest.of).getContent();
                totalCount = paymentRepository.countByMerchantAndCreatedAtBetweenAndStatus(
                    merchant, startInstant, endInstant, status);
            } else {
                payments = paymentRepository.findByMerchantAndCreatedAtBetween(
                    merchant, startInstant, endInstant,
                    org.springframework.data.domain.PageRequest.of).getContent();
                totalCount = paymentRepository.countByMerchantAndCreatedAtBetween(
                    merchant, startInstant, endInstant);
            }

            List<PaymentDetail> paymentDetails = payments.stream
                .map
                .collect);

            PaymentDetailsReport report = new PaymentDetailsReport(
                merchant.getId.toString(),
                merchant.getDocument,
                startDate,
                endDate,
                status,
                paymentDetails,
                totalCount,
                page,
                size,
                Instant.now
            );

            // Audit log
            auditService.logAction(merchant, "Report", "payment_details",
                AuditAction.VIEW, "Payment details report generated");

            return report;

        } catch  {
            log.error("Error generating payment details report for merchant {}: {}",
                merchant.getDocument, e.getMessage(), e);
            throw new BusinessException;
        }
    }

    @Timed
    public SettlementHistoryReport generateSettlementHistoryReport {
        log.info("Generating settlement history report for merchant: {} period: {} to {}",
            merchant.getDocument, startDate, endDate);

        try {
            List<Settlement> settlements = settlementRepository.findByMerchantAndSettlementDateBetween(
                merchant, startDate, endDate);

            List<SettlementDetail> settlementDetails = settlements.stream
                .map
                .collect);

            // Calculate totals
            BigDecimal totalGross = settlements.stream
                .map
                .reduce;

            BigDecimal totalFees = settlements.stream
                .map
                .reduce;

            BigDecimal totalNet = settlements.stream
                .map
                .reduce;

            SettlementHistoryReport report = new SettlementHistoryReport(
                merchant.getId.toString(),
                merchant.getDocument,
                startDate,
                endDate,
                settlementDetails,
                settlements.size,
                totalGross,
                totalFees,
                totalNet,
                Instant.now
            );

            // Audit log
            auditService.logAction(merchant, "Report", "settlement_history",
                AuditAction.VIEW, "Settlement history report generated");

            return report;

        } catch  {
            log.error("Error generating settlement history report for merchant {}: {}",
                merchant.getDocument, e.getMessage(), e);
            throw new BusinessException;
        }
    }

    @Timed
    public TransactionLedgerReport generateTransactionLedger(Merchant merchant, LocalDate startDate, LocalDate endDate,
                                                           TransactionType type, int page, int size) {
        log.info("Generating transaction ledger report for merchant: {} period: {} to {} type: {}",
            merchant.getDocument, startDate, endDate, type);

        try {
            Instant startInstant = startDate.atStartOfDay.toInstant();
            Instant endInstant = endDate.plusDays.atStartOfDay(ZoneOffset.UTC).toInstant();

            List<Transaction> transactions;
            long totalCount;

            if  {
                transactions = transactionRepository.findByMerchantAndCreatedAtBetweenAndType(
                    merchant, startInstant, endInstant, type,
                    org.springframework.data.domain.PageRequest.of).getContent();
                totalCount = transactionRepository.countByMerchantAndCreatedAtBetweenAndType(
                    merchant, startInstant, endInstant, type);
            } else {
                transactions = transactionRepository.findByMerchantAndCreatedAtBetween(
                    merchant, startInstant, endInstant,
                    org.springframework.data.domain.PageRequest.of).getContent();
                totalCount = transactionRepository.countByMerchantAndCreatedAtBetween(
                    merchant, startInstant, endInstant);
            }

            List<TransactionDetail> transactionDetails = transactions.stream
                .map
                .collect);

            TransactionLedgerReport report = new TransactionLedgerReport(
                merchant.getId.toString(),
                merchant.getDocument,
                startDate,
                endDate,
                type,
                transactionDetails,
                totalCount,
                page,
                size,
                Instant.now
            );

            // Audit log
            auditService.logAction(merchant, "Report", "transaction_ledger",
                AuditAction.VIEW, "Transaction ledger report generated");

            return report;

        } catch  {
            log.error("Error generating transaction ledger report for merchant {}: {}",
                merchant.getDocument, e.getMessage(), e);
            throw new BusinessException;
        }
    }

    @Async
    @Timed
    public CompletableFuture<byte[]> exportReportToCsv {
        log.info;

        try {
            List<String[]> csvData = convertReportToCsvData;
            byte[] csvBytes = generateCsvBytes;

            log.info;
            return CompletableFuture.completedFuture;

        } catch  {
            log.error, e);
            return CompletableFuture.failedFuture);
        }
    }

    @Async
    @Timed
    public CompletableFuture<byte[]> exportReportToJson {
        log.info;

        try {
            String jsonString = objectMapper.writeValueAsString;
            byte[] jsonBytes = jsonString.getBytes;

            log.info;
            return CompletableFuture.completedFuture;

        } catch  {
            log.error, e);
            return CompletableFuture.failedFuture);
        }
    }

    private PaymentStats calculatePaymentStats {
        Map<PaymentStatus, Long> statusCounts = new HashMap<>;
        Map<PaymentStatus, BigDecimal> statusAmounts = new HashMap<>;

        for ) {
            Long count = paymentRepository.countByMerchantAndCreatedAtBetweenAndStatus(
                merchant, startDate, endDate, status);
            statusCounts.put;

            BigDecimal amount = paymentRepository.sumAmountByMerchantAndCreatedAtBetweenAndStatus(
                merchant, startDate, endDate, status);
            statusAmounts.put;
        }

        long totalPayments = statusCounts.values.stream().mapToLong(Long::longValue).sum();
        BigDecimal totalAmount = statusAmounts.values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalPayments > 0 ?
            totalAmount.divide, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return new PaymentStats;
    }

    private TransactionStats calculateTransactionStats {
        List<Transaction> transactions = transactionRepository.findByMerchantAndCreatedAtBetween(
            merchant, startDate, endDate);

        Map<TransactionType, Long> typeCounts = transactions.stream
            .collect));

        Map<TransactionType, BigDecimal> typeAmounts = transactions.stream
            .collect(Collectors.groupingBy(Transaction::getType,
                Collectors.reducing));

        long totalTransactions = transactions.size;
        BigDecimal totalCredits = typeAmounts.getOrDefault;
        BigDecimal totalDebits = typeAmounts.getOrDefault;

        return new TransactionStats;
    }

    private SettlementStats calculateSettlementStats {
        LocalDate localStartDate = startDate.atZone.toLocalDate();
        LocalDate localEndDate = endDate.atZone.toLocalDate();

        List<Settlement> settlements = settlementRepository.findByMerchantAndSettlementDateBetween(
            merchant, localStartDate, localEndDate);

        long totalSettlements = settlements.size;
        BigDecimal totalGross = settlements.stream
            .map
            .reduce;

        BigDecimal totalFees = settlements.stream
            .map
            .reduce;

        BigDecimal totalNet = settlements.stream
            .map
            .reduce;

        return new SettlementStats;
    }

    private RevenueStats calculateRevenueStats {
        BigDecimal grossRevenue = paymentStats.getStatusAmounts.getOrDefault(PaymentStatus.APPROVED, BigDecimal.ZERO);
        BigDecimal fees = transactionStats.getTypeAmounts.getOrDefault(TransactionType.DEBIT, BigDecimal.ZERO);
        BigDecimal netRevenue = grossRevenue.subtract;
        BigDecimal feeRate = grossRevenue.compareTo > 0 ?
            fees.divide.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

        return new RevenueStats;
    }

    private PaymentDetail convertToPaymentDetail {
        return new PaymentDetail(
            payment.getId.toString(),
            payment.getReferenceId,
            payment.getAmount,
            payment.getCurrency,
            payment.getStatus,
            payment.getDescription,
            payment.getCreatedAt,
            payment.getProcessedAt,
            payment.getCustomerEmail
        );
    }

    private SettlementDetail convertToSettlementDetail {
        return new SettlementDetail(
            settlement.getId.toString(),
            settlement.getReferenceId,
            settlement.getGrossAmount,
            settlement.getFeeAmount,
            settlement.getNetAmount,
            settlement.getTransactionCount,
            settlement.getStatus,
            settlement.getSettlementDate,
            settlement.getCompletedAt
        );
    }

    private TransactionDetail convertToTransactionDetail {
        return new TransactionDetail(
            transaction.getId.toString(),
            transaction.getReferenceId,
            transaction.getType,
            transaction.getAmount,
            transaction.getDescription,
            transaction.getBalanceBefore,
            transaction.getBalanceAfter,
            transaction.getCreatedAt
        );
    }

    private List<String[]> convertReportToCsvData {
        List<String[]> csvData = new ArrayList<>;

        switch ) {
            case "payment_details":
                return convertPaymentDetailsReportToCsv report);
            case "settlement_history":
                return convertSettlementHistoryReportToCsv report);
            case "transaction_ledger":
                return convertTransactionLedgerReportToCsv report);
            default:
                throw new BusinessException;
        }
    }

    private List<String[]> convertPaymentDetailsReportToCsv {
        List<String[]> csvData = new ArrayList<>;

        // Header
        csvData.add(new String[]{
            "Payment ID", "Reference ID", "Amount", "Currency", "Status", "Description",
            "Created At", "Processed At", "Customer Email"
        });

        // Data
        for ) {
            csvData.add(new String[]{
                payment.getId,
                payment.getReferenceId,
                payment.getAmount.toString(),
                payment.getCurrency,
                payment.getStatus.name(),
                payment.getDescription,
                payment.getCreatedAt.toString(),
                payment.getProcessedAt != null ? payment.getProcessedAt().toString() : "",
                payment.getCustomerEmail
            });
        }

        return csvData;
    }

    private List<String[]> convertSettlementHistoryReportToCsv {
        List<String[]> csvData = new ArrayList<>;

        // Header
        csvData.add(new String[]{
            "Settlement ID", "Reference ID", "Gross Amount", "Fee Amount", "Net Amount",
            "Transaction Count", "Status", "Settlement Date", "Completed At"
        });

        // Data
        for ) {
            csvData.add(new String[]{
                settlement.getId,
                settlement.getReferenceId,
                settlement.getGrossAmount.toString(),
                settlement.getFeeAmount.toString(),
                settlement.getNetAmount.toString(),
                String.valueOf),
                settlement.getStatus.name(),
                settlement.getSettlementDate.toString(),
                settlement.getCompletedAt != null ? settlement.getCompletedAt().toString() : ""
            });
        }

        return csvData;
    }

    private List<String[]> convertTransactionLedgerReportToCsv {
        List<String[]> csvData = new ArrayList<>;

        // Header
        csvData.add(new String[]{
            "Transaction ID", "Reference ID", "Type", "Amount", "Description",
            "Balance Before", "Balance After", "Created At"
        });

        // Data
        for ) {
            csvData.add(new String[]{
                transaction.getId,
                transaction.getReferenceId,
                transaction.getType.name(),
                transaction.getAmount.toString(),
                transaction.getDescription,
                transaction.getBalanceBefore.toString(),
                transaction.getBalanceAfter.toString(),
                transaction.getCreatedAt.toString()
            });
        }

        return csvData;
    }

    private byte[] generateCsvBytes throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream;

        for  {
            String csvRow = String.join
                .map : "") + "\"")
                .toArray) + "\n";
            outputStream.write);
        }

        return outputStream.toByteArray;
    }

    // Report DTOs
    public static class FinancialSummaryReport {
        private final String merchantId;
        private final String merchantDocument;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final PaymentStats paymentStats;
        private final TransactionStats transactionStats;
        private final SettlementStats settlementStats;
        private final RevenueStats revenueStats;
        private final Instant generatedAt;

        public FinancialSummaryReport(String merchantId, String merchantDocument, LocalDate startDate, LocalDate endDate,
                                    PaymentStats paymentStats, TransactionStats transactionStats,
                                    SettlementStats settlementStats, RevenueStats revenueStats, Instant generatedAt) {
            this.merchantId = merchantId;
            this.merchantDocument = merchantDocument;
            this.startDate = startDate;
            this.endDate = endDate;
            this.paymentStats = paymentStats;
            this.transactionStats = transactionStats;
            this.settlementStats = settlementStats;
            this.revenueStats = revenueStats;
            this.generatedAt = generatedAt;
        }

        // Getters
        public String getMerchantId { return merchantId; }
        public String getMerchantDocument { return merchantDocument; }
        public LocalDate getStartDate { return startDate; }
        public LocalDate getEndDate { return endDate; }
        public PaymentStats getPaymentStats { return paymentStats; }
        public TransactionStats getTransactionStats { return transactionStats; }
        public SettlementStats getSettlementStats { return settlementStats; }
        public RevenueStats getRevenueStats { return revenueStats; }
        public Instant getGeneratedAt { return generatedAt; }
    }

    public static class PaymentDetailsReport {
        private final String merchantId;
        private final String merchantDocument;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final PaymentStatus status;
        private final List<PaymentDetail> payments;
        private final long totalCount;
        private final int page;
        private final int size;
        private final Instant generatedAt;

        public PaymentDetailsReport(String merchantId, String merchantDocument, LocalDate startDate, LocalDate endDate,
                                  PaymentStatus status, List<PaymentDetail> payments, long totalCount,
                                  int page, int size, Instant generatedAt) {
            this.merchantId = merchantId;
            this.merchantDocument = merchantDocument;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.payments = payments;
            this.totalCount = totalCount;
            this.page = page;
            this.size = size;
            this.generatedAt = generatedAt;
        }

        // Getters
        public String getMerchantId { return merchantId; }
        public String getMerchantDocument { return merchantDocument; }
        public LocalDate getStartDate { return startDate; }
        public LocalDate getEndDate { return endDate; }
        public PaymentStatus getStatus { return status; }
        public List<PaymentDetail> getPayments { return payments; }
        public long getTotalCount { return totalCount; }
        public int getPage { return page; }
        public int getSize { return size; }
        public Instant getGeneratedAt { return generatedAt; }
    }

    public static class SettlementHistoryReport {
        private final String merchantId;
        private final String merchantDocument;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final List<SettlementDetail> settlements;
        private final int totalSettlements;
        private final BigDecimal totalGross;
        private final BigDecimal totalFees;
        private final BigDecimal totalNet;
        private final Instant generatedAt;

        public SettlementHistoryReport(String merchantId, String merchantDocument, LocalDate startDate, LocalDate endDate,
                                     List<SettlementDetail> settlements, int totalSettlements,
                                     BigDecimal totalGross, BigDecimal totalFees, BigDecimal totalNet, Instant generatedAt) {
            this.merchantId = merchantId;
            this.merchantDocument = merchantDocument;
            this.startDate = startDate;
            this.endDate = endDate;
            this.settlements = settlements;
            this.totalSettlements = totalSettlements;
            this.totalGross = totalGross;
            this.totalFees = totalFees;
            this.totalNet = totalNet;
            this.generatedAt = generatedAt;
        }

        // Getters
        public String getMerchantId { return merchantId; }
        public String getMerchantDocument { return merchantDocument; }
        public LocalDate getStartDate { return startDate; }
        public LocalDate getEndDate { return endDate; }
        public List<SettlementDetail> getSettlements { return settlements; }
        public int getTotalSettlements { return totalSettlements; }
        public BigDecimal getTotalGross { return totalGross; }
        public BigDecimal getTotalFees { return totalFees; }
        public BigDecimal getTotalNet { return totalNet; }
        public Instant getGeneratedAt { return generatedAt; }
    }

    public static class TransactionLedgerReport {
        private final String merchantId;
        private final String merchantDocument;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final TransactionType type;
        private final List<TransactionDetail> transactions;
        private final long totalCount;
        private final int page;
        private final int size;
        private final Instant generatedAt;

        public TransactionLedgerReport(String merchantId, String merchantDocument, LocalDate startDate, LocalDate endDate,
                                     TransactionType type, List<TransactionDetail> transactions, long totalCount,
                                     int page, int size, Instant generatedAt) {
            this.merchantId = merchantId;
            this.merchantDocument = merchantDocument;
            this.startDate = startDate;
            this.endDate = endDate;
            this.type = type;
            this.transactions = transactions;
            this.totalCount = totalCount;
            this.page = page;
            this.size = size;
            this.generatedAt = generatedAt;
        }

        // Getters
        public String getMerchantId { return merchantId; }
        public String getMerchantDocument { return merchantDocument; }
        public LocalDate getStartDate { return startDate; }
        public LocalDate getEndDate { return endDate; }
        public TransactionType getType { return type; }
        public List<TransactionDetail> getTransactions { return transactions; }
        public long getTotalCount { return totalCount; }
        public int getPage { return page; }
        public int getSize { return size; }
        public Instant getGeneratedAt { return generatedAt; }
    }

    // Statistics classes
    public static class PaymentStats {
        private final long totalPayments;
        private final BigDecimal totalAmount;
        private final BigDecimal averageAmount;
        private final Map<PaymentStatus, Long> statusCounts;
        private final Map<PaymentStatus, BigDecimal> statusAmounts;

        public PaymentStats(long totalPayments, BigDecimal totalAmount, BigDecimal averageAmount,
                          Map<PaymentStatus, Long> statusCounts, Map<PaymentStatus, BigDecimal> statusAmounts) {
            this.totalPayments = totalPayments;
            this.totalAmount = totalAmount;
            this.averageAmount = averageAmount;
            this.statusCounts = statusCounts;
            this.statusAmounts = statusAmounts;
        }

        // Getters
        public long getTotalPayments { return totalPayments; }
        public BigDecimal getTotalAmount { return totalAmount; }
        public BigDecimal getAverageAmount { return averageAmount; }
        public Map<PaymentStatus, Long> getStatusCounts { return statusCounts; }
        public Map<PaymentStatus, BigDecimal> getStatusAmounts { return statusAmounts; }
    }

    public static class TransactionStats {
        private final long totalTransactions;
        private final BigDecimal totalCredits;
        private final BigDecimal totalDebits;
        private final Map<TransactionType, Long> typeCounts;
        private final Map<TransactionType, BigDecimal> typeAmounts;

        public TransactionStats(long totalTransactions, BigDecimal totalCredits, BigDecimal totalDebits,
                              Map<TransactionType, Long> typeCounts, Map<TransactionType, BigDecimal> typeAmounts) {
            this.totalTransactions = totalTransactions;
            this.totalCredits = totalCredits;
            this.totalDebits = totalDebits;
            this.typeCounts = typeCounts;
            this.typeAmounts = typeAmounts;
        }

        // Getters
        public long getTotalTransactions { return totalTransactions; }
        public BigDecimal getTotalCredits { return totalCredits; }
        public BigDecimal getTotalDebits { return totalDebits; }
        public Map<TransactionType, Long> getTypeCounts { return typeCounts; }
        public Map<TransactionType, BigDecimal> getTypeAmounts { return typeAmounts; }
    }

    public static class SettlementStats {
        private final long totalSettlements;
        private final BigDecimal totalGross;
        private final BigDecimal totalFees;
        private final BigDecimal totalNet;

        public SettlementStats {
            this.totalSettlements = totalSettlements;
            this.totalGross = totalGross;
            this.totalFees = totalFees;
            this.totalNet = totalNet;
        }

        // Getters
        public long getTotalSettlements { return totalSettlements; }
        public BigDecimal getTotalGross { return totalGross; }
        public BigDecimal getTotalFees { return totalFees; }
        public BigDecimal getTotalNet { return totalNet; }
    }

    public static class RevenueStats {
        private final BigDecimal grossRevenue;
        private final BigDecimal fees;
        private final BigDecimal netRevenue;
        private final BigDecimal feeRate;

        public RevenueStats {
            this.grossRevenue = grossRevenue;
            this.fees = fees;
            this.netRevenue = netRevenue;
            this.feeRate = feeRate;
        }

        // Getters
        public BigDecimal getGrossRevenue { return grossRevenue; }
        public BigDecimal getFees { return fees; }
        public BigDecimal getNetRevenue { return netRevenue; }
        public BigDecimal getFeeRate { return feeRate; }
    }

    // Detail classes
    public static class PaymentDetail {
        private final String id;
        private final String referenceId;
        private final BigDecimal amount;
        private final String currency;
        private final PaymentStatus status;
        private final String description;
        private final Instant createdAt;
        private final Instant processedAt;
        private final String customerEmail;

        public PaymentDetail(String id, String referenceId, BigDecimal amount, String currency, PaymentStatus status,
                           String description, Instant createdAt, Instant processedAt, String customerEmail) {
            this.id = id;
            this.referenceId = referenceId;
            this.amount = amount;
            this.currency = currency;
            this.status = status;
            this.description = description;
            this.createdAt = createdAt;
            this.processedAt = processedAt;
            this.customerEmail = customerEmail;
        }

        // Getters
        public String getId { return id; }
        public String getReferenceId { return referenceId; }
        public BigDecimal getAmount { return amount; }
        public String getCurrency { return currency; }
        public PaymentStatus getStatus { return status; }
        public String getDescription { return description; }
        public Instant getCreatedAt { return createdAt; }
        public Instant getProcessedAt { return processedAt; }
        public String getCustomerEmail { return customerEmail; }
    }

    public static class SettlementDetail {
        private final String id;
        private final String referenceId;
        private final BigDecimal grossAmount;
        private final BigDecimal feeAmount;
        private final BigDecimal netAmount;
        private final int transactionCount;
        private final com.zendapag.core.entity.enums.SettlementStatus status;
        private final LocalDate settlementDate;
        private final Instant completedAt;

        public SettlementDetail(String id, String referenceId, BigDecimal grossAmount, BigDecimal feeAmount,
                              BigDecimal netAmount, int transactionCount,
                              com.zendapag.core.entity.enums.SettlementStatus status,
                              LocalDate settlementDate, Instant completedAt) {
            this.id = id;
            this.referenceId = referenceId;
            this.grossAmount = grossAmount;
            this.feeAmount = feeAmount;
            this.netAmount = netAmount;
            this.transactionCount = transactionCount;
            this.status = status;
            this.settlementDate = settlementDate;
            this.completedAt = completedAt;
        }

        // Getters
        public String getId { return id; }
        public String getReferenceId { return referenceId; }
        public BigDecimal getGrossAmount { return grossAmount; }
        public BigDecimal getFeeAmount { return feeAmount; }
        public BigDecimal getNetAmount { return netAmount; }
        public int getTransactionCount { return transactionCount; }
        public com.zendapag.core.entity.enums.SettlementStatus getStatus { return status; }
        public LocalDate getSettlementDate { return settlementDate; }
        public Instant getCompletedAt { return completedAt; }
    }

    public static class TransactionDetail {
        private final String id;
        private final String referenceId;
        private final TransactionType type;
        private final BigDecimal amount;
        private final String description;
        private final BigDecimal balanceBefore;
        private final BigDecimal balanceAfter;
        private final Instant createdAt;

        public TransactionDetail(String id, String referenceId, TransactionType type, BigDecimal amount,
                               String description, BigDecimal balanceBefore, BigDecimal balanceAfter, Instant createdAt) {
            this.id = id;
            this.referenceId = referenceId;
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.balanceBefore = balanceBefore;
            this.balanceAfter = balanceAfter;
            this.createdAt = createdAt;
        }

        // Getters
        public String getId { return id; }
        public String getReferenceId { return referenceId; }
        public TransactionType getType { return type; }
        public BigDecimal getAmount { return amount; }
        public String getDescription { return description; }
        public BigDecimal getBalanceBefore { return balanceBefore; }
        public BigDecimal getBalanceAfter { return balanceAfter; }
        public Instant getCreatedAt { return createdAt; }
    }
}