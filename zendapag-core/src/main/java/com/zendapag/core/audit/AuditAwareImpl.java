package com.zendapag.core.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }

        String principal = authentication.getName();
        if (principal == null || "anonymousUser".equals(principal)) {
            return Optional.of("ANONYMOUS");
        }

        // Extract user information from different authentication types
        if (authentication.getPrincipal() instanceof String) {
            return Optional.of((String) authentication.getPrincipal());
        }

        // For API key authentication, extract from authorities or details
        if (authentication.getAuthorities() != null) {
            String apiKeyUser = authentication.getAuthorities().stream()
                    .filter(auth -> auth.getAuthority().startsWith("API_KEY_"))
                    .findFirst()
                    .map(auth -> auth.getAuthority().substring("API_KEY_".length()))
                    .orElse(null);

            if (apiKeyUser != null) {
                return Optional.of("API_KEY:" + apiKeyUser);
            }
        }

        // Extract from authentication details if available
        Object details = authentication.getDetails();
        if (details instanceof String) {
            return Optional.of((String) details);
        }

        return Optional.of(principal);
    }
}