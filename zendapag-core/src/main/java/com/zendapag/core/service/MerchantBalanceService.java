package com.zendapag.core.service;

import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Saldos do estabelecimento: totais (autoritativos, da conta) e a quebra por
 * método de pagamento, derivada do razão.
 *
 * - pendingTotal/availableTotal vêm de Account.pendingBalance/balance (o pool real,
 *   já refletindo saques/estornos).
 * - byMethod traz, por método: o PENDENTE (soma do líquido de PAYMENT não liberado,
 *   que reconcilia com pendingTotal) e o RECEBIDO/LIBERADO (soma do líquido de
 *   PAYMENT já liberado — bruto de saques/estornos, informativo).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantBalanceService {

    private final MerchantRepository merchantRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public record MethodBalance(String method, BigDecimal pending, BigDecimal received) {}
    public record MerchantBalances(BigDecimal availableTotal, BigDecimal pendingTotal, List<MethodBalance> byMethod) {}

    @Transactional(readOnly = true)
    public MerchantBalances getBalances(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));
        Account account = accountRepository.findByMerchant(merchant).stream().findFirst().orElse(null);
        if (account == null) {
            return new MerchantBalances(BigDecimal.ZERO, BigDecimal.ZERO, List.of());
        }

        BigDecimal availableTotal = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        BigDecimal pendingTotal = account.getPendingBalance() != null ? account.getPendingBalance() : BigDecimal.ZERO;

        Map<PaymentMethodType, BigDecimal> pending = toMap(transactionRepository.sumPaymentNetByMethod(account, false));
        Map<PaymentMethodType, BigDecimal> received = toMap(transactionRepository.sumPaymentNetByMethod(account, true));

        TreeSet<PaymentMethodType> methods = new TreeSet<>();
        methods.addAll(pending.keySet());
        methods.addAll(received.keySet());

        List<MethodBalance> byMethod = new ArrayList<>();
        for (PaymentMethodType m : methods) {
            byMethod.add(new MethodBalance(
                m.name(),
                pending.getOrDefault(m, BigDecimal.ZERO),
                received.getOrDefault(m, BigDecimal.ZERO)));
        }
        return new MerchantBalances(availableTotal, pendingTotal, byMethod);
    }

    private Map<PaymentMethodType, BigDecimal> toMap(List<Object[]> rows) {
        Map<PaymentMethodType, BigDecimal> map = new EnumMap<>(PaymentMethodType.class);
        for (Object[] row : rows) {
            // methodType nulo (lançamento legado) é tratado como PIX (default do motor)
            PaymentMethodType method = row[0] != null ? (PaymentMethodType) row[0] : PaymentMethodType.PIX;
            BigDecimal sum = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            map.merge(method, sum, BigDecimal::add);
        }
        return map;
    }
}
