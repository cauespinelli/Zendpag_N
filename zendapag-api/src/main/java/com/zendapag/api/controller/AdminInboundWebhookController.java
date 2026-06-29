package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import com.zendapag.core.entity.InboundWebhook;
import com.zendapag.core.repository.InboundWebhookRepository;
import com.zendapag.core.service.InboundWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Auditoria e reprocessamento dos webhooks de ENTRADA (ADMIN).
 */
@RestController
@RequestMapping("/api/v1/admin/inbound-webhooks")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminInboundWebhookController {

    private final InboundWebhookRepository inboundWebhookRepository;
    private final InboundWebhookService inboundWebhookService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<Map<String, Object>> body = inboundWebhookRepository
            .findAllByOrderByReceivedAtDesc(PageRequest.of(page, size))
            .map(this::toMap);
        return ResponseEntity.ok(ApiResponse.success("Webhooks recebidos", body));
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reprocess(@PathVariable Long id) {
        InboundWebhookService.ReceiveResult result = inboundWebhookService.reprocess(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", result.status().name());
        body.put("message", result.message());
        body.put("id", result.id());
        return ResponseEntity.ok(ApiResponse.success("Reprocessado", body));
    }

    private Map<String, Object> toMap(InboundWebhook w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("provider", w.getProvider());
        m.put("eventId", w.getEventId());
        m.put("eventType", w.getEventType() != null ? w.getEventType().name() : null);
        m.put("referenceId", w.getReferenceId());
        m.put("status", w.getStatus().name());
        m.put("signatureValid", w.isSignatureValid());
        m.put("attempts", w.getAttempts());
        m.put("errorMessage", w.getErrorMessage());
        m.put("receivedAt", w.getReceivedAt());
        m.put("processedAt", w.getProcessedAt());
        return m;
    }
}
