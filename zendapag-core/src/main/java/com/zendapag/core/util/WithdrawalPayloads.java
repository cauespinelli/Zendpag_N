package com.zendapag.core.util;

import com.zendapag.core.entity.PixWithdrawal;

import java.util.HashMap;
import java.util.Map;

/**
 * Fonte única do payload de webhook de saque (contrato com o gateway):
 * { event, withdrawal_id, reference_id, status, amount, net, merchant_id }.
 * O `status` sempre reflete o estado atual do saque — garantindo que bate com o
 * nome do evento (WITHDRAWAL_PROCESSING/COMPLETED/FAILED).
 */
public final class WithdrawalPayloads {

    private WithdrawalPayloads() {}

    public static Map<String, Object> of(PixWithdrawal w, String eventType) {
        Map<String, Object> body = new HashMap<>();
        body.put("event", eventType);
        body.put("withdrawal_id", w.getId().toString());
        body.put("reference_id", w.getReferenceId());
        body.put("status", w.getStatus().name());
        body.put("amount", w.getAmount());
        body.put("net", w.getNetAmount());
        body.put("merchant_id", w.getMerchant().getId().toString());
        return body;
    }
}
