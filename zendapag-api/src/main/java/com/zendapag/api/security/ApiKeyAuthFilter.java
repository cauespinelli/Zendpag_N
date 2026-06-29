package com.zendapag.api.security;

import com.zendapag.core.entity.Origin;
import com.zendapag.core.service.OriginService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autenticação por API Key para ORIGENS externas (gateway). Lê o header
 * X-API-Key; se válido, autentica como a origem com a authority ROLE_ORIGIN e o
 * principal = código da origem (ex.: ONE_A_ONE).
 *
 * Sem header, não faz nada (deixa o filtro JWT seguir). Com header inválido, não
 * autentica — o endpoint protegido por ROLE_ORIGIN responde 401.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";

    private final OriginService originService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (apiKey != null && !apiKey.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Origin origin = originService.authenticate(apiKey).orElse(null);
            if (origin != null) {
                var auth = new UsernamePasswordAuthenticationToken(
                    origin.getCode(), null, List.of(new SimpleGrantedAuthority("ROLE_ORIGIN")));
                auth.setDetails(origin.getCode());
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("[ApiKeyAuth] autenticado como origem {}", origin.getCode());
            } else {
                log.warn("[ApiKeyAuth] API Key inválida (prefixo {}…)",
                    apiKey.length() > 8 ? apiKey.substring(0, 8) : apiKey);
            }
        }
        chain.doFilter(request, response);
    }
}
