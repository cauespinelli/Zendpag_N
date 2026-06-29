package com.zendapag.core.service;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.entity.enums.WithdrawalStatus;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PixWithdrawalRepository;
import com.zendapag.core.service.payout.PayoutProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ciclo de vida do saque: aprovar debita (PROCESSING) e conclui (COMPLETED), com
 * os eventos na ordem e o status batendo; falha do PSP estorna o saldo (FAILED).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PixWithdrawalService — ciclo do saque (PROCESSING → COMPLETED / FAILED)")
class PixWithdrawalServiceTest {

    @Mock private PixWithdrawalRepository withdrawalRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private WebhookService webhookService;
    @Mock private LedgerService ledgerService;
    @Mock private PayoutProvider payoutProvider;

    @InjectMocks private PixWithdrawalService service;

    private Merchant merchant;
    private PixWithdrawal withdrawal;

    @BeforeEach
    void setUp() {
        merchant = new Merchant("L", "11222333000144", "l@l.com");
        merchant.setId(UUID.randomUUID());

        Account account = new Account();
        account.setMerchant(merchant);
        account.setBalance(new BigDecimal("1000.00"));

        withdrawal = new PixWithdrawal("WD-1", account, merchant, new BigDecimal("300.00"), "11222333000144", "CNPJ");
        withdrawal.setId(UUID.randomUUID());

        lenient().when(withdrawalRepository.findById(withdrawal.getId())).thenReturn(Optional.of(withdrawal));
        lenient().when(withdrawalRepository.save(any(PixWithdrawal.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("aprovar com sucesso: debita, dispara PROCESSING depois COMPLETED, status final COMPLETED")
    void aprovaComSucesso() {
        when(payoutProvider.send(any(PixWithdrawal.class))).thenReturn(PayoutProvider.PayoutResult.ok("E2E1"));

        service.approveWithdrawal(withdrawal.getId());

        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.COMPLETED);
        // debitou o disponível (via ledger) ao entrar em PROCESSING
        verify(ledgerService).debitForWithdrawal(any(), eq(new BigDecimal("300.00")), eq("WD-1"), any());
        verify(ledgerService, never()).reverseWithdrawal(any(), any(), any(), any());
        // eventos: PROCESSING e COMPLETED (status bate com o nome)
        verify(webhookService).notifyMerchant(eq(merchant), eq("WITHDRAWAL_PROCESSING"), any());
        verify(webhookService).notifyMerchant(eq(merchant), eq("WITHDRAWAL_COMPLETED"), any());
    }

    @Test
    @DisplayName("aprovar com falha do PSP: estorna o saldo e dispara WITHDRAWAL_FAILED, status FAILED")
    void aprovaComFalhaEstorna() {
        when(payoutProvider.send(any(PixWithdrawal.class)))
            .thenReturn(PayoutProvider.PayoutResult.fail("recusado pelo PSP"));

        service.approveWithdrawal(withdrawal.getId());

        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.FAILED);
        verify(ledgerService).debitForWithdrawal(any(), any(), any(), any());      // debitou ao processar
        verify(ledgerService).reverseWithdrawal(any(), eq(new BigDecimal("300.00")), eq("WD-1"), any()); // estornou
        verify(webhookService).notifyMerchant(eq(merchant), eq("WITHDRAWAL_FAILED"), any());
        verify(webhookService, never()).notifyMerchant(eq(merchant), eq("WITHDRAWAL_COMPLETED"), any());
    }

    @Test
    @DisplayName("só saques PENDING podem ser aprovados")
    void soPendingAprova() {
        withdrawal.startProcessing(); // já PROCESSING
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.approveWithdrawal(withdrawal.getId()))
            .isInstanceOf(com.zendapag.common.exception.BusinessException.class)
            .hasMessageContaining("PENDING");
    }
}
