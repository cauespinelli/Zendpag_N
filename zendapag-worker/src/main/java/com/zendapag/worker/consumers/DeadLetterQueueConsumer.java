package com.zendapag.worker.consumers;

import com.zendapag.core.events.DomainEvent;
import com.zendapag.worker.services.DeadLetterQueueService;
import com.zendapag.worker.services.AlertingService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Kafka consumer for dead letter queue processing.
 */
@Component
@KafkaListener(
    topics = "dead-letter-queue",
    groupId = "dead-letter-processor",
    containerFactory = "deadLetterQueueContainerFactory"
)
public class DeadLetterQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueConsumer.class);

    private final DeadLetterQueueService deadLetterQueueService;
    private final AlertingService alertingService;
    private final MeterRegistry meterRegistry;

    private final Counter dlqCounter;
    private final Counter alertCounter;
    private final Timer processingTimer;

    public DeadLetterQueueConsumer(DeadLetterQueueService deadLetterQueueService,
                                 AlertingService alertingService,
                                 MeterRegistry meterRegistry) {
        this.deadLetterQueueService = deadLetterQueueService;
        this.alertingService = alertingService;
        this.meterRegistry = meterRegistry;

        this.dlqCounter = Counter.builder("kafka.dlq.events.received").register(meterRegistry);
        this.alertCounter = Counter.builder("kafka.dlq.alerts.sent").register(meterRegistry);
        this.processingTimer = Timer.builder("kafka.dlq.processing.time").register(meterRegistry);
    }

    @KafkaListener(topics = "dead-letter-queue")
    public void handleDeadLetterEvent(@Payload Object event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String originalTopic,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset,
                                    @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage,
                                    @Header(value = KafkaHeaders.EXCEPTION_STACKTRACE, required = false) String stackTrace,
                                    @Header(value = "kafka_original-topic", required = false) String kafkaOriginalTopic,
                                    Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String eventType = event.getClass().getSimpleName();
            String eventId = extractEventId(event);
            String merchantId = extractMerchantId(event);

            log.error("Processing dead letter event: type={}, id={}, merchantId={}, originalTopic={}, partition={}, offset={}, error={}",
                    eventType, eventId, merchantId, kafkaOriginalTopic, partition, offset, exceptionMessage);

            String dlqRecordId = deadLetterQueueService.storeDlqEvent(
                event, eventType, eventId, merchantId,
                kafkaOriginalTopic != null ? kafkaOriginalTopic : originalTopic,
                exceptionMessage, stackTrace, Instant.now()
            );

            String failureCategory = categorizeFailure(exceptionMessage, stackTrace);

            if (shouldAlert(eventType, failureCategory, merchantId)) {
                sendAlert(eventType, eventId, merchantId, failureCategory, exceptionMessage, dlqRecordId);
            }

            dlqCounter.increment();
            checkForSystemIssues(eventType, failureCategory, merchantId);

            log.info("Dead letter event processed: dlqRecordId={}, eventType={}, category={}",
                    dlqRecordId, eventType, failureCategory);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process dead letter event: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        } finally {
            sample.stop(processingTimer);
        }
    }

    private String extractEventId(Object event) {
        if (event instanceof DomainEvent) {
            return ((DomainEvent) event).getEventId();
        }
        try {
            var method = event.getClass().getMethod("getEventId");
            return (String) method.invoke(event);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractMerchantId(Object event) {
        if (event instanceof DomainEvent) {
            return (String) ((DomainEvent) event).getMetadataValue("merchant_id");
        }
        try {
            var method = event.getClass().getMethod("getMerchantId");
            return (String) method.invoke(event);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String categorizeFailure(String exceptionMessage, String stackTrace) {
        if (exceptionMessage == null) return "unknown";
        String message = exceptionMessage.toLowerCase();
        if (message.contains("timeout")) return "timeout";
        if (message.contains("connection") || message.contains("network")) return "network";
        if (message.contains("serialization")) return "serialization";
        if (message.contains("validation")) return "validation";
        if (message.contains("database") || message.contains("sql")) return "database";
        return "application";
    }

    private boolean shouldAlert(String eventType, String failureCategory, String merchantId) {
        if (eventType.contains("PaymentCompleted") || eventType.contains("SettlementCompleted")) return true;
        if ("database".equals(failureCategory) || "network".equals(failureCategory)) return true;
        return "validation".equals(failureCategory) && merchantId != null && !merchantId.equals("unknown");
    }

    private void sendAlert(String eventType, String eventId, String merchantId,
                          String failureCategory, String errorMessage, String dlqRecordId) {
        try {
            Map<String, Object> alertData = Map.of(
                "event_type", eventType,
                "event_id", eventId,
                "merchant_id", merchantId != null ? merchantId : "unknown",
                "failure_category", failureCategory,
                "error_message", errorMessage != null ? errorMessage : "No error message",
                "dlq_record_id", dlqRecordId,
                "timestamp", Instant.now().toString()
            );
            alertingService.sendDlqAlert("Dead Letter Queue Alert",
                String.format("Event %s (%s) failed processing", eventType, eventId), alertData);
            alertCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send DLQ alert: {}", e.getMessage(), e);
        }
    }

    private void checkForSystemIssues(String eventType, String failureCategory, String merchantId) {
        try {
            long recentFailures = deadLetterQueueService.countRecentFailures(
                failureCategory, java.time.Duration.ofMinutes(15));
            if (recentFailures > 10) {
                alertingService.sendSystemAlert("High DLQ Volume Alert",
                    String.format("High volume of %s failures: %d events in 15 min", failureCategory, recentFailures),
                    Map.of("failure_category", failureCategory, "failure_count", recentFailures, "time_window", "15_minutes"));
            }
        } catch (Exception e) {
            log.error("Failed to check system issues: {}", e.getMessage(), e);
        }
    }
}
