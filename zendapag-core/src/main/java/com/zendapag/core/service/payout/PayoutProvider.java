package com.zendapag.core.service.payout;

import com.zendapag.core.entity.PixWithdrawal;

/**
 * Abstração de envio de PIX (saque) para um provedor externo (BaaS/PSP).
 * O resto do sistema fala só com esta interface; trocar o sandbox pelo PSP
 * real é fornecer outro adapter, sem mexer no motor.
 */
public interface PayoutProvider {

    /** Envia o saque ao provedor. Não lança em falha de negócio — retorna o resultado. */
    PayoutResult send(PixWithdrawal withdrawal);

    /** Resultado do envio: sucesso + endToEndId do PIX, ou falha + motivo. */
    record PayoutResult(boolean success, String endToEndId, String message) {
        public static PayoutResult ok(String endToEndId) {
            return new PayoutResult(true, endToEndId, "OK");
        }
        public static PayoutResult fail(String message) {
            return new PayoutResult(false, null, message);
        }
    }
}
