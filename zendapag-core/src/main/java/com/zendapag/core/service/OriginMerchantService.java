package com.zendapag.core.service;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.User;
import com.zendapag.core.entity.enums.MerchantStatus;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Provisiona estabelecimentos vindos de uma ORIGEM externa (gateway), marcados
 * com o `source` da origem. Cria também a conta do estabelecimento (necessária
 * para o motor financeiro) e é IDEMPOTENTE pelo documento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OriginMerchantService {

    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.0199");

    private final MerchantRepository merchantRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Merchant provision(String source, String name, String document, String email, String phone,
                              String externalId, String webhookUrl) {
        // Idempotência: documento já existe -> devolve o mesmo estabelecimento (sem cache)
        Merchant existing = merchantRepository.findByDocumentUncached(document).orElse(null);
        if (existing != null) {
            log.info("[OriginMerchant] documento {} já existe (source {}) — idempotente", document, existing.getSource());
            return existing;
        }

        Merchant m = new Merchant(name, document, email);
        m.setPhone(phone);
        m.setSource(source);
        m.setSourceExternalId(externalId);
        m.setWebhookUrl(webhookUrl);
        m.setStatus(MerchantStatus.ACTIVE);
        m.setKycVerified(true); // sandbox: aprovado de imediato
        m.setFeeRate(DEFAULT_FEE_RATE);
        m.setTradingName(name);
        m = merchantRepository.save(m);

        // Conta do estabelecimento (exigida pelo motor para crédito/saldo)
        User owner = systemUser();
        String accNumber = "ACC-ORIG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        accountRepository.save(Account.builder()
            .accountNumber(accNumber)
            .type(Account.AccountType.PAYMENT)
            .balance(BigDecimal.ZERO)
            .pendingBalance(BigDecimal.ZERO)
            .pixKey(document)
            .pixKeyType(Account.PixKeyType.CNPJ)
            .status(Account.AccountStatus.ACTIVE)
            .user(owner)
            .merchant(m)
            .build());

        log.info("[OriginMerchant] estabelecimento {} criado (source {}, externalId {})",
            m.getId(), source, externalId);
        return m;
    }

    /** Usuário-sistema dono das contas provisionadas por origem (find-or-create). */
    private User systemUser() {
        return userRepository.findByUsername("origin_system").orElseGet(() ->
            userRepository.save(User.builder()
                .username("origin_system")
                .email("origin-system@zendpag.com")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName("Origin System")
                .cpfCnpj("00000000000000")
                .status(User.UserStatus.ACTIVE)
                .roles(Set.of(User.Role.USER))
                .build()));
    }
}
