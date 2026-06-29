package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.core.entity.Origin;
import com.zendapag.core.repository.OriginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * Gerência das ORIGENS (contratos de origem) e da autenticação por API Key.
 *
 * A API Key é gerada aleatória, devolvida em texto puro UMA vez e guardada
 * apenas como SHA-256. A autenticação recalcula o hash do header e busca a origem.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OriginService {

    public static final String SOURCE_DIRETO = "DIRETO";

    private final OriginRepository originRepository;

    /** Em DEV, key fixa do ONE_A_ONE para testes reproduzíveis (logada na subida). */
    @Value("${zendapag.origins.one-a-one.dev-key:zk_one_a_one_devkey00000000000000}")
    private String oneAOneDevKey;
    /** URL do receptor de webhook do gateway (de volta). Default = dev-origin-sink. */
    @Value("${zendapag.origins.one-a-one.webhook-url:http://localhost:8093/api/v1/webhooks/receive/dev-origin-sink}")
    private String oneAOneWebhookUrl;
    @Value("${zendapag.origins.one-a-one.webhook-secret:whsec_origin_one_dev}")
    private String oneAOneWebhookSecret;

    /** Resultado de criação/rotação: a origem + a key em texto puro (mostrada uma vez). */
    public record OriginKey(Origin origin, String plaintextKey) {}

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaults() {
        // Origem interna (estabelecimentos próprios) — sem API Key.
        if (!originRepository.existsByCode(SOURCE_DIRETO)) {
            Origin direto = new Origin(SOURCE_DIRETO, "Direto (Zend)");
            originRepository.save(direto);
            log.info("[Origin] origem DIRETO semeada");
        }
        // Origem externa de exemplo (gateway One A One) — com key fixa de DEV.
        if (!originRepository.existsByCode("ONE_A_ONE")) {
            Origin one = new Origin("ONE_A_ONE", "Gateway One A One");
            one.setApiKeyHash(sha256Hex(oneAOneDevKey));
            one.setApiKeyPrefix(prefixOf(oneAOneDevKey));
            one.setWebhookUrl(oneAOneWebhookUrl);
            one.setWebhookSecret(oneAOneWebhookSecret);
            originRepository.save(one);
            log.warn("[Origin] origem ONE_A_ONE semeada — API Key de DEV: {} (webhook -> {})",
                oneAOneDevKey, oneAOneWebhookUrl);
        }
    }

    /** Autentica uma chamada externa pela API Key (header). Null/ inválida -> empty. */
    @Transactional(readOnly = true)
    public java.util.Optional<Origin> authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return java.util.Optional.empty();
        }
        return originRepository.findByApiKeyHashAndActiveTrue(sha256Hex(rawApiKey.trim()));
    }

    @Transactional(readOnly = true)
    public Origin requireByCode(String code) {
        return originRepository.findByCode(code)
            .orElseThrow(() -> new BusinessException("Origem não encontrada: " + code));
    }

    @Transactional(readOnly = true)
    public List<Origin> list() {
        return originRepository.findAll();
    }

    /** Cria uma origem nova e gera a API Key (texto puro devolvido uma vez). */
    @Transactional
    public OriginKey createOrigin(String code, String name, String webhookUrl, String webhookSecret) {
        if (originRepository.existsByCode(code)) {
            throw new BusinessException("Já existe origem com o código: " + code);
        }
        Origin origin = new Origin(code, name);
        String key = generateKey(code);
        origin.setApiKeyHash(sha256Hex(key));
        origin.setApiKeyPrefix(prefixOf(key));
        origin.setWebhookUrl(webhookUrl);
        origin.setWebhookSecret(webhookSecret != null ? webhookSecret : "whsec_" + UUID.randomUUID().toString().replace("-", ""));
        origin = originRepository.save(origin);
        log.info("[Origin] origem {} criada (prefixo {})", code, origin.getApiKeyPrefix());
        return new OriginKey(origin, key);
    }

    /** Rotaciona a API Key de uma origem (nova key em texto puro). */
    @Transactional
    public OriginKey rotateKey(String code) {
        Origin origin = requireByCode(code);
        String key = generateKey(code);
        origin.setApiKeyHash(sha256Hex(key));
        origin.setApiKeyPrefix(prefixOf(key));
        originRepository.save(origin);
        log.info("[Origin] API Key da origem {} rotacionada", code);
        return new OriginKey(origin, key);
    }

    private String generateKey(String code) {
        String slug = code.toLowerCase().replaceAll("[^a-z0-9]", "_");
        String rand = UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "zk_" + slug + "_" + rand;
    }

    private String prefixOf(String key) {
        return key.length() > 12 ? key.substring(0, 12) : key;
    }

    /** SHA-256 em hex do texto. */
    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] raw = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular SHA-256", e);
        }
    }
}
