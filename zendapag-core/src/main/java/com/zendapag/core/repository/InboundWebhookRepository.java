package com.zendapag.core.repository;

import com.zendapag.core.entity.InboundWebhook;
import com.zendapag.core.entity.enums.InboundWebhookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InboundWebhookRepository extends JpaRepository<InboundWebhook, Long> {

    /** Chave de idempotência: um evento por provider. */
    Optional<InboundWebhook> findByProviderAndEventId(String provider, String eventId);

    Page<InboundWebhook> findAllByOrderByReceivedAtDesc(Pageable pageable);

    Page<InboundWebhook> findByStatusOrderByReceivedAtDesc(InboundWebhookStatus status, Pageable pageable);
}
