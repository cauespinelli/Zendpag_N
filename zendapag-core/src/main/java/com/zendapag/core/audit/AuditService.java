package com.zendapag.core.audit;

import com.zendapag.core.entity.AuditLog;
import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.enums.AuditAction;
import com.zendapag.core.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String entityType, String entityId, AuditAction action) {
        logAction(null, entityType, entityId, action, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Merchant merchant, String entityType, String entityId, AuditAction action) {
        logAction(merchant, entityType, entityId, action, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String entityType, String entityId, AuditAction action, String description) {
        logAction(null, entityType, entityId, action, description, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Merchant merchant, String entityType, String entityId, AuditAction action, String description) {
        logAction(merchant, entityType, entityId, action, description, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String entityType, String entityId, AuditAction action,
                         Map<String, Object> oldValues, Map<String, Object> newValues) {
        logAction(null, entityType, entityId, action, null, oldValues, newValues);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Merchant merchant, String entityType, String entityId, AuditAction action,
                         Map<String, Object> oldValues, Map<String, Object> newValues) {
        logAction(merchant, entityType, entityId, action, null, oldValues, newValues);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Merchant merchant, String entityType, String entityId, AuditAction action,
                         String description, Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            AuditLog auditLog = new AuditLog(merchant, entityType, entityId, action);
            auditLog.setDescription(description);

            // Set old and new values
            if (oldValues != null && !oldValues.isEmpty()) {
                auditLog.setOldValues(oldValues);
            }
            if (newValues != null && !newValues.isEmpty()) {
                auditLog.setNewValues(newValues);
            }

            // Set user context from security context
            setUserContext(auditLog);

            // Set request context if available
            setRequestContext(auditLog);

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            // Log error but don't propagate to avoid breaking business operations
            System.err.println("Failed to save audit log: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(String entityType, String entityId, AuditAction action, String errorMessage) {
        logFailure(null, entityType, entityId, action, errorMessage, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(Merchant merchant, String entityType, String entityId, AuditAction action,
                          String errorMessage, Exception exception) {
        try {
            AuditLog auditLog = new AuditLog(merchant, entityType, entityId, action);
            auditLog.markAsFailure(errorMessage);

            if (exception != null) {
                auditLog.addMetadata("exception_class", exception.getClass().getSimpleName());
                auditLog.addMetadata("stack_trace", getStackTrace(exception));
            }

            setUserContext(auditLog);
            setRequestContext(auditLog);

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            System.err.println("Failed to save audit failure log: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuspiciousActivity(String entityType, String entityId, AuditAction action,
                                    String reason, Integer riskScore) {
        logSuspiciousActivity(null, entityType, entityId, action, reason, riskScore);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuspiciousActivity(Merchant merchant, String entityType, String entityId, AuditAction action,
                                    String reason, Integer riskScore) {
        try {
            AuditLog auditLog = new AuditLog(merchant, entityType, entityId, action);
            auditLog.markSuspicious(reason);

            if (riskScore != null) {
                auditLog.setRiskScore(riskScore);
            }

            setUserContext(auditLog);
            setRequestContext(auditLog);

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            System.err.println("Failed to save suspicious activity log: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logApiAccess(String apiKeyId, String endpoint, String method, long processingTime,
                           boolean success, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog("API", "endpoint", action(method));
            auditLog.setDescription("API access to " + endpoint);
            auditLog.setApiKeyId(apiKeyId);
            auditLog.setProcessingTimeMs(processingTime);
            auditLog.setSuccess(success);

            if (!success && errorMessage != null) {
                auditLog.setErrorMessage(errorMessage);
            }

            setRequestContext(auditLog);
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            System.err.println("Failed to save API access log: " + e.getMessage());
        }
    }

    private void setUserContext(AuditLog auditLog) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            if (!"anonymousUser".equals(username)) {
                auditLog.setUserId(username);
                auditLog.setUserName(username);

                // Determine user type based on authentication
                if (authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().startsWith("API_KEY_"))) {
                    auditLog.setUserType("API");
                } else if (authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                    auditLog.setUserType("ADMIN");
                } else if (authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_MERCHANT"))) {
                    auditLog.setUserType("MERCHANT");
                } else {
                    auditLog.setUserType("USER");
                }
            }
        }
    }

    private void setRequestContext(AuditLog auditLog) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            auditLog.setIpAddress(getClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setRequestUri(request.getRequestURI());
            auditLog.setSessionId(request.getSession(false) != null ?
                request.getSession().getId() : null);

            // Set correlation ID from headers if present
            String correlationId = request.getHeader("X-Correlation-ID");
            if (correlationId != null) {
                auditLog.setCorrelationId(correlationId);
            }

            String requestId = request.getHeader("X-Request-ID");
            if (requestId != null) {
                auditLog.setRequestId(requestId);
            }
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, get the first one
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String getStackTrace(Exception exception) {
        if (exception == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getSimpleName()).append(": ")
          .append(exception.getMessage()).append("\n");

        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 2000) { // Limit stack trace length
                sb.append("\t... (truncated)");
                break;
            }
        }

        return sb.toString();
    }

    private AuditAction action(String httpMethod) {
        switch (httpMethod.toUpperCase()) {
            case "POST": return AuditAction.CREATE;
            case "PUT":
            case "PATCH": return AuditAction.UPDATE;
            case "DELETE": return AuditAction.DELETE;
            case "GET": return AuditAction.VIEW;
            default: return AuditAction.VIEW;
        }
    }
}