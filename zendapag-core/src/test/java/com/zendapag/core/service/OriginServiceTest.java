package com.zendapag.core.service;

import com.zendapag.core.entity.Origin;
import com.zendapag.core.repository.OriginRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Autenticação por API Key da origem: a key em texto puro é resolvida pelo
 * SHA-256; key inválida/ausente não autentica.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OriginService — autenticação por API Key (SHA-256)")
class OriginServiceTest {

    @Mock private OriginRepository repo;
    @InjectMocks private OriginService service;

    @Test
    @DisplayName("SHA-256 é determinístico e key diferente → hash diferente")
    void hashDeterministico() {
        String h1 = OriginService.sha256Hex("zk_one_a_one_dev");
        String h2 = OriginService.sha256Hex("zk_one_a_one_dev");
        String h3 = OriginService.sha256Hex("zk_outra_key");
        assertThat(h1).isEqualTo(h2).hasSize(64);
        assertThat(h1).isNotEqualTo(h3);
    }

    @Test
    @DisplayName("key válida resolve a origem ativa pelo hash")
    void keyValidaAutentica() {
        String key = "zk_one_a_one_devkey00000000000000";
        Origin origin = new Origin("ONE_A_ONE", "Gateway");
        when(repo.findByApiKeyHashAndActiveTrue(OriginService.sha256Hex(key)))
            .thenReturn(Optional.of(origin));

        assertThat(service.authenticate(key)).isPresent().get()
            .extracting(Origin::getCode).isEqualTo("ONE_A_ONE");
    }

    @Test
    @DisplayName("key inválida → não autentica (empty)")
    void keyInvalida() {
        // repo retorna empty (default) para qualquer hash não cadastrado
        assertThat(service.authenticate("chave-errada")).isEmpty();
    }

    @Test
    @DisplayName("key nula/vazia → não autentica e nem consulta o banco")
    void keyNula() {
        assertThat(service.authenticate(null)).isEmpty();
        assertThat(service.authenticate("  ")).isEmpty();
    }
}
