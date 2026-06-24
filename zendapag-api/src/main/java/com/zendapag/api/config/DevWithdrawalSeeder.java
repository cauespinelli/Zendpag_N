package com.zendapag.api.config;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.entity.User;
import com.zendapag.core.entity.enums.MerchantStatus;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PixWithdrawalRepository;
import com.zendapag.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Seeder de dados de teste para DESENVOLVIMENTO: cria um saque PIX em status
 * PENDING (com o estabelecimento, conta e usuário necessários), para que a
 * tela de Saques do Painel Admin não apareça vazia no primeiro login.
 *
 * Só roda no perfil "dev". Idempotente: se o saque de teste já existir
 * (mesmo referenceId), não recria nada.
 */
@Slf4j
@Component
@Profile("dev")
@Order(20) // roda depois do DevAdminSeeder (admin), embora seja independente
@RequiredArgsConstructor
public class DevWithdrawalSeeder implements CommandLineRunner {

    private static final String SEED_REF = "WD-SEED-0001";

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final AccountRepository accountRepository;
    private final PixWithdrawalRepository withdrawalRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (withdrawalRepository.findByReferenceId(SEED_REF).isPresent()) {
            log.info("[DevWithdrawalSeeder] Saque de teste já existe ({}) — nada a fazer.", SEED_REF);
            return;
        }

        // 1) Usuário dono do estabelecimento (seller)
        User seller = userRepository.findByEmail("loja.aurora@zendpag.com").orElseGet(() ->
            userRepository.save(User.builder()
                .username("loja_aurora")
                .email("loja.aurora@zendpag.com")
                .password(passwordEncoder.encode("Seller@12345"))
                .fullName("Loja Aurora Digital")
                .cpfCnpj("12345678000190")
                .status(User.UserStatus.ACTIVE)
                .roles(Set.of(User.Role.MERCHANT, User.Role.USER))
                .build()));

        // 2) Estabelecimento (merchant), ativo e com KYC
        Merchant merchant = merchantRepository.findByDocument("12345678000190").orElseGet(() -> {
            Merchant m = new Merchant("Loja Aurora Digital", "12345678000190", "financeiro@auroradigital.com.br");
            m.setStatus(MerchantStatus.ACTIVE);
            m.setKycVerified(true);
            m.setPhone("+5511987654321");
            return merchantRepository.save(m);
        });

        // 3) Conta de pagamento com saldo (para o saque ter de onde sair)
        Account account = accountRepository.findByAccountNumber("ACC-DEV-0001").orElseGet(() ->
            accountRepository.save(Account.builder()
                .accountNumber("ACC-DEV-0001")
                .type(Account.AccountType.PAYMENT)
                .balance(new BigDecimal("250000.00"))
                .pendingBalance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .user(seller)
                .merchant(merchant)
                .build()));

        // 4) Saque PIX em PENDING (aparece na fila de aprovação)
        PixWithdrawal w = new PixWithdrawal(
            SEED_REF, account, merchant,
            new BigDecimal("15000.00"), "12345678000190", "CNPJ");
        w.setRecipientName("Loja Aurora Digital");
        w.setRecipientDocument("12345678000190");
        w.setFeeAmount(new BigDecimal("2.50"));
        w.setBalanceBefore(account.getBalance());
        w.setBalanceAfter(account.getBalance().subtract(w.getAmount()));
        w.setDescription("Saque de teste (seed dev)");
        // status já é PENDING (default) e requestedAt já vem do construtor
        withdrawalRepository.save(w);

        log.warn("[DevWithdrawalSeeder] Saque PENDING de teste criado: {} — R$ 15.000,00 (Loja Aurora Digital).", SEED_REF);
    }
}
