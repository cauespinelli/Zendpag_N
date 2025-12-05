package com.zendapag.core.service;

import com.zendapag.core.entity.Merchant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ReconciliationService {

    public void performReconciliation(LocalDate date) {
        log.info("Performing reconciliation for date: {}", date);
        // Implementation will be added
    }

    public String generateReconciliationSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Generating reconciliation summary from {} to {}", startDate, endDate);
        // Implementation will be added
        return "Reconciliation Summary";
    }
}
