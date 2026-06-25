package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.Transaction;
import com.zendapag.core.entity.enums.TransactionType;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do razão (saldo + lançamentos). Repositórios mockados; assertamos a
 * mutação de saldo e os lançamentos (PAYMENT, FEE, REFUND) gerados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerService — saldo e lançamentos")
class LedgerServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private LedgerService ledger;

    private Merchant merchant;
    private Account account;
    private Payment payment;

    @BeforeEach
    void setUp() {
        merchant = new Merchant("Loja Teste", "12345678000190", "loja@teste.com");
        merchant.setId(UUID.randomUUID());

        account = new Account();
        account.setBalance(new BigDecimal("1000.00"));
        account.setMerchant(merchant);

        payment = new Payment("PAY-001", merchant, new BigDecimal("100.00"));
        payment.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("aprovação credita o LÍQUIDO no saldo do estabelecimento")
    void creditaLiquidoNoSaldo() {
        when(accountRepository.findByMerchant(merchant)).thenReturn(List.of(account));

        ledger.settleApprovedPayment(payment,
            new BigDecimal("100.00"), new BigDecimal("1.99"), new BigDecimal("98.01"));

        // 1000,00 + 98,01 (líquido) = 1098,01
        assertThat(account.getBalance()).isEqualByComparingTo("1098.01");
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("a taxa é registrada como lançamento FEE (receita) e o líquido como PAYMENT")
    void registraLancamentosPaymentEFee() {
        when(accountRepository.findByMerchant(merchant)).thenReturn(List.of(account));

        ledger.settleApprovedPayment(payment,
            new BigDecimal("100.00"), new BigDecimal("1.99"), new BigDecimal("98.01"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();

        Transaction paymentTxn = saved.stream().filter(t -> t.getType() == TransactionType.PAYMENT).findFirst().orElseThrow();
        Transaction feeTxn = saved.stream().filter(t -> t.getType() == TransactionType.FEE).findFirst().orElseThrow();

        assertThat(paymentTxn.getAmount()).isEqualByComparingTo("100.00");
        assertThat(paymentTxn.getFeeAmount()).isEqualByComparingTo("1.99");
        assertThat(paymentTxn.getNetAmount()).isEqualByComparingTo("98.01");

        // A taxa (MDR) entra como receita da plataforma, valor = taxa
        assertThat(feeTxn.getAmount()).isEqualByComparingTo("1.99");
    }

    @Test
    @DisplayName("estorno DEBITA o líquido do saldo e a taxa fica retida (não volta)")
    void estornoRevertesomenteLiquido() {
        when(accountRepository.findByMerchant(merchant)).thenReturn(List.of(account));

        ledger.reverseRefund(payment, new BigDecimal("98.01"));

        // 1000,00 − 98,01 = 901,99 (apenas o líquido é revertido; a taxa de 1,99 permanece como receita)
        assertThat(account.getBalance()).isEqualByComparingTo("901.99");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction refund = captor.getValue();
        assertThat(refund.getType()).isEqualTo(TransactionType.REFUND);
        assertThat(refund.getNetAmount()).isEqualByComparingTo("-98.01"); // débito (negativo)
    }

    @Test
    @DisplayName("saldo nulo é tratado como zero ao creditar")
    void saldoNuloTratadoComoZero() {
        account.setBalance(null);
        when(accountRepository.findByMerchant(merchant)).thenReturn(List.of(account));

        ledger.settleApprovedPayment(payment,
            new BigDecimal("100.00"), new BigDecimal("1.99"), new BigDecimal("98.01"));

        assertThat(account.getBalance()).isEqualByComparingTo("98.01");
    }

    @Test
    @DisplayName("estabelecimento sem conta → BusinessException ao creditar")
    void semContaLancaErro() {
        when(accountRepository.findByMerchant(merchant)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> ledger.settleApprovedPayment(payment,
            new BigDecimal("100.00"), new BigDecimal("1.99"), new BigDecimal("98.01")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("sem conta");
    }
}
