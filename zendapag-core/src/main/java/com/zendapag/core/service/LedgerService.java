package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.TransactionStatus;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Razão financeiro (motor): movimenta saldo das contas e registra lançamentos
 * de transação. Centraliza o crédito/débito para que pagamento, saque e
 * settlement compartilhem a mesma lógica de saldo + auditoria.
 *
 * Sandbox: tudo síncrono e em uma transação. Sem PSP — a confirmação do
 * pagamento é simulada pelo endpoint de aprovação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Liquida um pagamento aprovado: credita o líquido na conta do
     * estabelecimento e registra dois lançamentos — o crédito do pagamento
     * (líquido ao merchant) e a taxa (receita da plataforma).
     */
    @Transactional
    public void settleApprovedPayment(Payment payment, BigDecimal gross, BigDecimal fee, BigDecimal net) {
        Merchant merchant = payment.getMerchant();

        Account account = accountRepository.findByMerchant(merchant).stream()
            .findFirst()
            .orElseThrow(() -> new BusinessException(
                "Estabelecimento sem conta para crédito: " + merchant.getId()));

        // 1) Movimenta o saldo (a conta do estabelecimento recebe o líquido)
        BigDecimal before = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        account.setBalance(before.add(net));
        accountRepository.save(account);
        log.info("Saldo do estabelecimento {} creditado: {} -> {} (+{})",
            merchant.getId(), before, account.getBalance(), net);

        // 2) Lançamento do crédito do pagamento (líquido ao estabelecimento)
        Transaction credit = new Transaction("TXN-" + payment.getReferenceId(), merchant, payment,
            TransactionType.PAYMENT, gross);
        credit.setAccount(account);
        credit.setFeeAmount(fee);
        credit.setNetAmount(net);
        credit.setStatus(TransactionStatus.COMPLETED);
        credit.setDescription("Pagamento aprovado — crédito líquido ao estabelecimento");
        transactionRepository.save(credit);

        // 3) Receita da plataforma (taxa / MDR)
        Transaction platformFee = new Transaction("FEE-" + payment.getReferenceId(), merchant, payment,
            TransactionType.FEE, fee);
        platformFee.setAccount(account);
        platformFee.setStatus(TransactionStatus.COMPLETED);
        platformFee.setDescription("Taxa da plataforma (MDR) sobre o pagamento");
        transactionRepository.save(platformFee);

        log.info("Lançamentos registrados: PAYMENT {} (líquido {}) + FEE {} (receita)",
            credit.getReferenceId(), net, fee);
    }

    /**
     * Reverte o crédito de um pagamento estornado: debita o líquido do saldo do
     * estabelecimento e registra um lançamento REFUND. A taxa (MDR) já cobrada
     * permanece como receita da plataforma (política comum de estorno).
     */
    @Transactional
    public void reverseRefund(Payment payment, BigDecimal net) {
        Merchant merchant = payment.getMerchant();
        Account account = accountRepository.findByMerchant(merchant).stream()
            .findFirst()
            .orElseThrow(() -> new BusinessException(
                "Estabelecimento sem conta para débito: " + merchant.getId()));

        BigDecimal before = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        BigDecimal amount = net != null ? net : BigDecimal.ZERO;
        account.setBalance(before.subtract(amount));
        accountRepository.save(account);
        log.info("Estorno — saldo do estabelecimento {}: {} -> {} (-{})",
            merchant.getId(), before, account.getBalance(), amount);

        Transaction refund = new Transaction("REF-" + payment.getReferenceId(), merchant, payment,
            TransactionType.REFUND, amount);
        refund.setAccount(account);
        refund.setNetAmount(amount.negate());
        refund.setStatus(TransactionStatus.COMPLETED);
        refund.setDescription("Estorno do pagamento — débito do líquido ao estabelecimento");
        transactionRepository.save(refund);
    }
}
