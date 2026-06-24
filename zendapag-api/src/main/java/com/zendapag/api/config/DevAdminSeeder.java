package com.zendapag.api.config;

import com.zendapag.core.entity.User;
import com.zendapag.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeder de usuário ADMIN para AMBIENTE DE DESENVOLVIMENTO.
 *
 * Roda apenas no perfil "dev" (perfil ativo padrão). Cria um usuário com papel
 * ADMIN ao subir a aplicação, para destravar o login no Painel Admin Master —
 * já que o banco de dev (H2) sobe vazio a cada execução e o cadastro público
 * (/auth/register) cria apenas usuário comum.
 *
 * NÃO roda em staging/prod (sem @Profile correspondente). Idempotente: se o
 * e-mail já existir, não recria.
 *
 * Credenciais padrão (sobrescrevíveis por variável de ambiente / properties):
 *   zendapag.dev.admin.email     (default: admin@zendpag.com)
 *   zendapag.dev.admin.password  (default: Admin@12345)
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevAdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${zendapag.dev.admin.email:admin@zendpag.com}")
    private String adminEmail;

    @Value("${zendapag.dev.admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${zendapag.dev.admin.username:admin}")
    private String adminUsername;

    @Override
    public void run(String... args) {
        try {
            seed();
        } catch (Exception e) {
            // Um seeder de dev NUNCA deve impedir o app de subir.
            log.warn("[DevAdminSeeder] Não foi possível criar o usuário admin: {}. App segue normalmente.", e.getMessage());
        }
    }

    private void seed() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("[DevAdminSeeder] Usuário admin de dev já existe: {} — nada a fazer.", adminEmail);
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .fullName("Admin Master")
                .cpfCnpj("00000000000")
                .status(User.UserStatus.ACTIVE)
                .roles(Set.of(User.Role.ADMIN, User.Role.USER))
                .build();

        userRepository.save(admin);

        log.warn("====================================================================");
        log.warn("[DevAdminSeeder] Usuário ADMIN de DEV criado (NÃO use em produção):");
        log.warn("[DevAdminSeeder]   e-mail:  {}", adminEmail);
        log.warn("[DevAdminSeeder]   usuário: {}", adminUsername);
        log.warn("[DevAdminSeeder]   senha:   {}", adminPassword);
        log.warn("====================================================================");
    }
}
