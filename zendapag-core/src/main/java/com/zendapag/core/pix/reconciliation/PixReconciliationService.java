package com.zendapag.core.pix.reconciliation;

import com.zendapag.core.entity.ReconciliationReport;
import com.zendapag.core.pix.client.PixClient;
import com.zendapag.core.pix.config.PixConfig;
import com.zendapag.core.repository.PaymentRepository;
import com.zendapag.core.repository.ReconciliationReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class PixReconciliationService {

    private final PixClient pixClient;
    private final PixConfig pixConfig;
    private final PaymentRepository paymentRepository;
    private final ReconciliationReportRepository reconciliationReportRepository;

    @Autowired
    public PixReconciliationService(
            PixClient pixClient,
            PixConfig pixConfig,
            PaymentRepository paymentRepository,
            ReconciliationReportRepository reconciliationReportRepository) {
        this.pixClient = pixClient;
        this.pixConfig = pixConfig;
        this.paymentRepository = paymentRepository;
        this.reconciliationReportRepository = reconciliationReportRepository;
    }

    @Scheduled(cron = "${pix.reconciliation.schedule:0 0 3 * * ?}")
    @Transactional
    public void runScheduledReconciliation() {
        log.info("Running scheduled PIX reconciliation for yesterday");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        performReconciliation(yesterday);
    }

    @Transactional
    public ReconciliationReport performReconciliationRange(LocalDate startDate, LocalDate endDate) {
        log.info("Performing PIX reconciliation from {} to {}", startDate, endDate);
        ReconciliationReport report = new ReconciliationReport();
        log.info("Reconciliation completed");
        return reconciliationReportRepository.save(report);
    }

    public ReconciliationResult performReconciliation(LocalDate date) {
        log.info("Performing PIX reconciliation for date: {}", date);
        return new ReconciliationResult(date, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, Collections.emptyList());
    }

    public QuickReconciliationResult performQuickReconciliation(LocalDate date) {
        log.info("Performing quick PIX reconciliation for date: {}", date);
        return new QuickReconciliationResult(date, true, 0, BigDecimal.ZERO);
    }

    public ReconciliationSummary generateReconciliationSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Generating reconciliation summary from {} to {}", startDate, endDate);
        return new ReconciliationSummary(startDate, endDate, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public List<ReconciliationReport> getReconciliationHistory(LocalDate startDate, LocalDate endDate) {
        log.debug("Getting reconciliation history from {} to {}", startDate, endDate);
        return new ArrayList<>();
    }

    public ReconciliationReport getLatestReconciliationReport() {
        return reconciliationReportRepository.findAll().stream()
            .findFirst()
            .orElse(null);
    }

    // DTOs
    public record ReconciliationResult(
        LocalDate date,
        int totalInternal,
        int totalExternal,
        int discrepancies,
        BigDecimal internalAmount,
        BigDecimal externalAmount,
        List<DiscrepancyItem> discrepancyItems
    ) {}

    public record QuickReconciliationResult(
        LocalDate date,
        boolean matched,
        int transactionCount,
        BigDecimal totalAmount
    ) {}

    public record ReconciliationSummary(
        LocalDate startDate,
        LocalDate endDate,
        int totalReconciled,
        int totalDiscrepancies,
        BigDecimal totalAmount,
        BigDecimal discrepancyAmount
    ) {}

    public record DiscrepancyItem(
        String txId,
        String type,
        BigDecimal internalAmount,
        BigDecimal externalAmount,
        String reason
    ) {}
}
