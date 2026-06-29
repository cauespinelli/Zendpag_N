package com.zendapag.core.service;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.PayoutScope;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Liberação de saldo retido: o job move o líquido de PENDENTE para DISPONÍVEL no
 * vencimento, marca o lançamento como liberado e registra um RELEASE de auditoria.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceReleaseService — liberação pendente → disponível")
class BalanceReleaseServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PayoutPolicyService payoutPolicyService;
    @Mock private AutoPayoutService autoPayoutService;

    @InjectMocks private BalanceReleaseService service;

    @Test
    @DisplayName("lançamento vencido move o líquido de pendente p/ disponível, marca released e registra RELEASE")
    void liberaVencido() {
        Merchant merchant = new Merchant("L", "00000000000191", "l@l.com");
        merchant.setId(UUID.randomUUID());

        Account account = new Account();
        account.setMerchant(merchant);
        account.setBalance(new BigDecimal("0.00"));
        account.setPendingBalance(new BigDecimal("100.00"));

        Transaction txn = new Transaction("TXN-PAY-1", merchant, null, TransactionType.PAYMENT, new BigDecimal("100.00"));
        txn.setAccount(account);
        txn.setNetAmount(new BigDecimal("100.00"));
        txn.setReleased(false);

        when(transactionRepository.findDueForRelease(any(Instant.class))).thenReturn(List.of(txn));
        // método PIX (payment nulo) com saque automático DESLIGADO → não dispara auto-payout
        when(payoutPolicyService.resolve(any(Merchant.class), any(PaymentMethodType.class)))
            .thenReturn(new PayoutPolicyService.EffectiveRule(PaymentMethodType.PIX, true, 1, false, PayoutScope.GLOBAL));

        int released = service.releaseDuePending();

        assertThat(released).isEqualTo(1);
        assertThat(account.getPendingBalance()).isEqualByComparingTo("0.00");   // saiu do pendente
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");        // entrou no disponível
        assertThat(txn.getReleased()).isTrue();

        // registra o lançamento RELEASE de auditoria
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(t -> t.getType() == TransactionType.RELEASE);

        // saque automático NÃO dispara (regra desligada)
        verify(autoPayoutService, never()).maybeAutoPayout(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    @DisplayName("nada vencido → libera zero")
    void nadaVencido() {
        when(transactionRepository.findDueForRelease(any(Instant.class))).thenReturn(List.of());
        assertThat(service.releaseDuePending()).isZero();
    }
}
