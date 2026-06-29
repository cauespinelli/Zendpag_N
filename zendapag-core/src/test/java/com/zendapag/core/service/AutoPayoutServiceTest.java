package com.zendapag.core.service;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.PixWithdrawalRepository;
import com.zendapag.core.repository.TransactionRepository;
import com.zendapag.core.service.payout.PayoutProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Saque automático (auto-payout): dispara quando há saldo disponível e a regra
 * permite, respeitando o gating (regra, master switch, chave PIX, saldo).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutoPayoutService — disparo e gating do saque automático")
class AutoPayoutServiceTest {

    @Mock private PixWithdrawalRepository withdrawalRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private PayoutProvider payoutProvider;
    @Mock private WebhookService webhookService;

    @InjectMocks private AutoPayoutService service;

    private Merchant merchant;
    private Account account;

    @BeforeEach
    void setUp() {
        merchant = new Merchant("L", "11222333000144", "l@l.com");
        merchant.setId(UUID.randomUUID());
        merchant.setAutoSettle(true);

        account = new Account();
        account.setMerchant(merchant);
        account.setBalance(new BigDecimal("1000.00"));
        account.setPixKey("11222333000144");
        account.setPixKeyType(Account.PixKeyType.CNPJ);

        // o save atribui o id (como o banco) — necessário pro payload do webhook
        lenient().when(withdrawalRepository.save(any(PixWithdrawal.class))).thenAnswer(i -> {
            PixWithdrawal w = i.getArgument(0);
            if (w.getId() == null) w.setId(UUID.randomUUID());
            return w;
        });
    }

    @Test
    @DisplayName("habilitado + chave + saldo → cria saque AUTOMATIC, debita e dispara WITHDRAWAL_COMPLETED")
    void disparaQuandoHabilitado() {
        when(payoutProvider.send(any(PixWithdrawal.class)))
            .thenReturn(PayoutProvider.PayoutResult.ok("E2E123"));

        service.maybeAutoPayout(merchant, account, new BigDecimal("500.00"), PaymentMethodType.PIX, true);

        assertThat(account.getBalance()).isEqualByComparingTo("500.00"); // 1000 - 500
        verify(withdrawalRepository).save(any(PixWithdrawal.class));
        verify(webhookService).notifyMerchant(eq(merchant), eq("WITHDRAWAL_COMPLETED"), any());
        verify(webhookService).notifyOrigin(eq(merchant), eq("WITHDRAWAL_COMPLETED"), any());
    }

    @Test
    @DisplayName("regra com saque automático DESLIGADO → não saca")
    void naoSacaSeRegraDesligada() {
        service.maybeAutoPayout(merchant, account, new BigDecimal("500.00"), PaymentMethodType.PIX, false);
        verify(withdrawalRepository, never()).save(any());
        verify(payoutProvider, never()).send(any());
    }

    @Test
    @DisplayName("master switch do merchant (autoSettle=false) → não saca mesmo com a regra ligada")
    void naoSacaSeMasterSwitchDesligado() {
        merchant.setAutoSettle(false);
        service.maybeAutoPayout(merchant, account, new BigDecimal("500.00"), PaymentMethodType.PIX, true);
        verify(withdrawalRepository, never()).save(any());
    }

    @Test
    @DisplayName("conta sem chave PIX → não saca (pulado)")
    void naoSacaSemChavePix() {
        account.setPixKey(null);
        service.maybeAutoPayout(merchant, account, new BigDecimal("500.00"), PaymentMethodType.PIX, true);
        verify(withdrawalRepository, never()).save(any());
    }

    @Test
    @DisplayName("saldo insuficiente → não saca")
    void naoSacaSemSaldo() {
        account.setBalance(new BigDecimal("100.00"));
        service.maybeAutoPayout(merchant, account, new BigDecimal("500.00"), PaymentMethodType.PIX, true);
        verify(withdrawalRepository, never()).save(any());
    }
}
