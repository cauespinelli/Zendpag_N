package com.zendapag.core.service;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.User;
import com.zendapag.core.repository.AccountRepository;
import com.zendapag.core.repository.MerchantRepository;
import com.zendapag.core.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Provisionamento de estabelecimentos de uma origem externa: marca o `source`,
 * cria a conta e é idempotente pelo documento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OriginMerchantService — criar estabelecimento por origem (idempotente)")
class OriginMerchantServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private OriginMerchantService service;

    @Test
    @DisplayName("documento novo → cria estabelecimento com source e provisiona a conta")
    void criaNovo() {
        when(merchantRepository.findByDocumentUncached("12312312000199")).thenReturn(Optional.empty());
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByUsername("origin_system")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(passwordEncoder.encode(any())).thenReturn("hash");

        Merchant m = service.provision("ONE_A_ONE", "Loja Gateway", "12312312000199",
            "loja@g.com", "11999990000", "NUVRA-M-001", "https://wh");

        assertThat(m.getSource()).isEqualTo("ONE_A_ONE");
        assertThat(m.getSourceExternalId()).isEqualTo("NUVRA-M-001");
        verify(accountRepository).save(any(Account.class)); // provisionou a conta
    }

    @Test
    @DisplayName("documento já existente → idempotente: devolve o mesmo e NÃO cria de novo")
    void idempotentePorDocumento() {
        Merchant existente = new Merchant("Loja Gateway", "12312312000199", "loja@g.com");
        existente.setId(UUID.randomUUID());
        existente.setSource("ONE_A_ONE");
        when(merchantRepository.findByDocumentUncached("12312312000199")).thenReturn(Optional.of(existente));

        Merchant m = service.provision("ONE_A_ONE", "Loja Gateway", "12312312000199",
            "loja@g.com", null, "NUVRA-M-001", null);

        assertThat(m).isSameAs(existente);
        verify(merchantRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }
}
