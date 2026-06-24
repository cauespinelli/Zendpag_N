package com.zendapag.api.config;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.PixWithdrawal;
import com.zendapag.core.entity.User;
import com.zendapag.core.entity.enums.MerchantStatus;
import com.zendapag.core.entity.enums.PaymentStatus;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.PaymentRepository;
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
 * Seeder ÚNICO de dados de teste para DESENVOLVIMENTO. Cria estabelecimentos
 * (cada um com feeRate, webhookUrl e uma conta de saldo), pagamentos e um saque
 * PENDING — para as telas do Painel Admin e o motor financeiro terem dados.
 *
 * Só no perfil "dev". Idempotente (checa um referenceId de pagamento conhecido).
 */
@Slf4j
@Component
@Profile("dev")
@Order(10)
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PixWithdrawalRepository withdrawalRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            seed();
        } catch (Exception e) {
            log.warn("[DevDataSeeder] Não foi possível criar os dados de teste: {}. App segue normalmente.", e.getMessage());
        }
    }

    private void seed() {
        if (paymentRepository.findByReferenceId("PAY-SEED-0001").isPresent()) {
            log.info("[DevDataSeeder] Dados de teste já existem — nada a fazer.");
            return;
        }

        // Usuário dono das contas (em dev, um só usuário possui todas as contas)
        User seller = userRepository.findByEmail("loja.aurora@zendpag.com").orElseGet(() ->
            userRepository.save(User.builder()
                .username("loja_aurora")
                .email("loja.aurora@zendpag.com")
                .password(passwordEncoder.encode("Seller@12345"))
                .fullName("Loja Aurora Digital")
                .cpfCnpj("12345678000191")
                .status(User.UserStatus.ACTIVE)
                .roles(Set.of(User.Role.MERCHANT, User.Role.USER))
                .build()));

        // 1) Estabelecimentos (com taxa, webhook e conta)
        Merchant aurora = merchant("Loja Aurora Digital", "12345678000190", "financeiro@auroradigital.com.br", "0.0199");
        Merchant edu = merchant("EduMaster Cursos", "98765432000121", "contato@edumaster.com.br", "0.0290");
        Merchant fit = merchant("FitShop Suplementos", "45612378000155", "adm@fitshop.com.br", "0.0349");
        Merchant gamer = merchant("GamerZone Store", "11222333000144", "suporte@gamerzone.com.br", "0.0199");

        Account auroraAcc = account(seller, aurora, "ACC-DEV-0001", new BigDecimal("250000.00"));
        account(seller, edu, "ACC-DEV-0002", BigDecimal.ZERO);
        account(seller, fit, "ACC-DEV-0003", BigDecimal.ZERO);
        account(seller, gamer, "ACC-DEV-0004", BigDecimal.ZERO);

        // 2) Pagamentos
        pay("PAY-SEED-0001", aurora, "Carlos Henrique", "12345678900", "459.90", "4.60", PaymentStatus.APPROVED);
        pay("PAY-SEED-0002", edu, "Mariana Lopes", "98765432100", "1299.00", "45.46", PaymentStatus.APPROVED);
        pay("PAY-SEED-0003", fit, "Roberto Dias", "45612378900", "289.90", "0.00", PaymentStatus.REJECTED);
        pay("PAY-SEED-0004", aurora, "Ana Beatriz", "32165498700", "749.00", "3.49", PaymentStatus.PENDING);
        pay("PAY-SEED-0005", gamer, "Juliana Castro", "65498732100", "89.90", "0.90", PaymentStatus.PENDING);
        pay("PAY-SEED-0006", edu, "Camila Reis", "25836914700", "497.00", "17.40", PaymentStatus.REFUNDED);
        pay("PAY-SEED-0007", fit, "Pedro Antunes", "14725836900", "2150.00", "75.25", PaymentStatus.APPROVED);
        pay("PAY-SEED-0008", gamer, "Beatriz Nunes", "74185296300", "899.00", "0.00", PaymentStatus.PENDING);

        // 3) Saque PENDING de teste (conta da Aurora)
        PixWithdrawal w = new PixWithdrawal("WD-SEED-0001", auroraAcc, aurora,
            new BigDecimal("15000.00"), "12345678000190", "CNPJ");
        w.setRecipientName("Loja Aurora Digital");
        w.setRecipientDocument("12345678000190");
        w.setFeeAmount(new BigDecimal("2.50"));
        w.setBalanceBefore(auroraAcc.getBalance());
        w.setBalanceAfter(auroraAcc.getBalance().subtract(w.getAmount()));
        w.setDescription("Saque de teste (seed dev)");
        withdrawalRepository.save(w);

        log.warn("[DevDataSeeder] Dados de teste criados: 4 estabelecimentos (c/ conta+taxa), 8 pagamentos e 1 saque PENDING.");
    }

    private Merchant merchant(String name, String document, String email, String feeRate) {
        return merchantRepository.findByDocument(document).orElseGet(() -> {
            Merchant m = new Merchant(name, document, email);
            m.setStatus(MerchantStatus.ACTIVE);
            m.setKycVerified(true);
            m.setTradingName(name);
            m.setFeeRate(new BigDecimal(feeRate));
            m.setWebhookUrl("https://example.com/zendapag/webhook/" + document);
            m.setWebhookSecret("whsec_dev_" + document);
            return merchantRepository.save(m);
        });
    }

    private Account account(User user, Merchant merchant, String number, BigDecimal balance) {
        return accountRepository.findByAccountNumber(number).orElseGet(() ->
            accountRepository.save(Account.builder()
                .accountNumber(number)
                .type(Account.AccountType.PAYMENT)
                .balance(balance)
                .pendingBalance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .user(user)
                .merchant(merchant)
                .build()));
    }

    private void pay(String ref, Merchant merchant, String customerName, String customerDoc,
                     String amount, String fee, PaymentStatus status) {
        Payment p = new Payment(ref, merchant, new BigDecimal(amount));
        p.setStatus(status);
        p.setCustomerName(customerName);
        p.setCustomerDocument(customerDoc);
        p.setCustomerEmail(customerName.toLowerCase().replace(" ", ".") + "@cliente.com");
        BigDecimal feeAmount = new BigDecimal(fee);
        p.setFeeAmount(feeAmount);
        p.setNetAmount(new BigDecimal(amount).subtract(feeAmount));
        paymentRepository.save(p);
    }
}
