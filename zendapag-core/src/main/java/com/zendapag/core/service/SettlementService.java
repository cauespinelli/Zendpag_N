package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Settlement;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.SettlementRepository;
import com.zendapag.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Liquidação (repasse) — consolida os pagamentos aprovados e ainda não
 * liquidados de um estabelecimento num {@link Settlement}.
 *
 * Modelo (sandbox): a aprovação do pagamento já credita o líquido no saldo
 * disponível (ver PaymentEngineService/LedgerService); o settlement agrupa
 * esses lançamentos PAYMENT num lote de repasse, calcula bruto/taxa/líquido e
 * marca cada transação como liquidada (vinculada ao settlement), de modo que
 * não sejam liquidadas de novo. Num modelo futuro com saldo pendente, este é
 * o ponto que moveria pendente -> disponível.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;

    /**
     * Cria e liquida um lote (repasse) com todos os pagamentos aprovados e
     * pendentes de liquidação do estabelecimento.
     */
    @Transactional
    public Settlement settleMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));

        List<Transaction> pending = transactionRepository.findUnsettledPaymentsByMerchant(merchant);
        if (pending.isEmpty()) {
            throw new BusinessException("Nenhum pagamento pendente de liquidação para o estabelecimento " + merchantId);
        }

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        LocalDate periodStart = LocalDate.now();
        LocalDate periodEnd = LocalDate.now();

        for (Transaction t : pending) {
            gross = gross.add(orZero(t.getAmount()));
            fee = fee.add(orZero(t.getFeeAmount()));
            net = net.add(orZero(t.getNetAmount()));
            if (t.getTransactionDate() != null && t.getTransactionDate().isBefore(periodStart)) {
                periodStart = t.getTransactionDate();
            }
        }

        Settlement settlement = new Settlement(merchant, generateReferenceId(), LocalDate.now(), periodStart, periodEnd);
        // Vincula cada transação ao settlement (também atualiza contadores)
        pending.forEach(settlement::addTransaction);
        settlement.setGrossAmount(gross); // recalcula totalAmount/netAmount = gross - fee
        settlement.setFeeAmount(fee);
        settlement.setNetAmount(net);
        settlement.setDescription("Repasse de " + pending.size() + " pagamento(s) ao estabelecimento");

        // Fluxo de status: PENDING -> PROCESSING -> SETTLED
        settlement.process();
        settlement.settle();

        Settlement saved = settlementRepository.save(settlement);
        transactionRepository.saveAll(pending); // persiste o vínculo (settlement_id) em cada transação

        log.info("Settlement {} criada e liquidada — {} pagamentos, bruto {}, taxa {}, líquido {}",
            saved.getReferenceId(), pending.size(), gross, fee, net);
        return saved;
    }

    /**
     * Processa um settlement existente (PENDING -> PROCESSING -> SETTLED).
     * Usado quando o lote já foi criado (ex.: pelo worker).
     */
    @Transactional
    public void processSettlement(String settlementId) throws BusinessException {
        log.info("Processing settlement: {}", settlementId);
        Settlement settlement = settlementRepository.findById(UUID.fromString(settlementId))
            .orElseThrow(() -> new ResourceNotFoundException("Settlement", "id", settlementId));
        if (settlement.isPending()) {
            settlement.process();
        }
        if (settlement.isProcessing()) {
            settlement.settle();
        }
        settlementRepository.save(settlement);
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String generateReferenceId() {
        return "STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
