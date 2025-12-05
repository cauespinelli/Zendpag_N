package com.zendapag.core.service;

import com.zendapag.core.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RiskService {

    public String assessRisk(Payment payment) {
        log.info("Assessing risk for payment: {}", payment.getId());
        // Implementation will be added
        return "LOW_RISK";
    }
}
