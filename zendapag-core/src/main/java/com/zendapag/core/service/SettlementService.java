package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    public void processSettlement(String settlementId) throws BusinessException {
        log.info("Processing settlement: {}", settlementId);
        // Implementation will be added
    }

    public void validateSettlementPeriod(String settlementId) throws ResourceNotFoundException {
        log.info("Validating settlement period: {}", settlementId);
        // Implementation will be added
    }
}
