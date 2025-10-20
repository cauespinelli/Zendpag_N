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
 * Handles failed events that couldn't be processed after all retry attempts.
 * This consumer focuses on logging, alerting, and manual intervention tracking.
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

    // Metrics
    private final Counter dlqCounter;
    private final Counter alertCounter;
    private final Timer processingTimer;

    public DeadLetterQueueConsumer(DeadLetterQueueService deadLetterQueueService,
                                 AlertingService alertingService,
                                 MeterRegistry meterRegistry) {
        this.deadLetterQueueService = deadLetterQueueService;
        this.alertingService = alertingService;

        this.dlqCounter = Counter.builder("kafka.dlq.events.received")
                .register(meterRegistry);
        this.alertCounter = Counter.builder("kafka.dlq.alerts.sent")
                .register(meterRegistry);
        this.processingTimer = Timer.builder("kafka.dlq.processing.time")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "dead-letter-queue")
    public void handleDeadLetterEvent(@Payload Object event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String originalTopic,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset,
                                    @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage,
                                    @Header(value = KafkaHeaders.EXCEPTION_STACKTRACE, required = false) String stackTrace,
                                    @Header(value = "kafka_original-topic", required = false) String kafkaOriginalTopic,
                                    Acknowledgment acknowledgment) {

        Timer.Sample sample = Timer.start();
        try {
            String eventType = event.getClass().getSimpleName();
            String eventId = extractEventId(event);
            String merchantId = extractMerchantId(event);

            log.error("Processing dead letter event: type={}, id={}, merchantId={}, originalTopic={}, partition={}, offset={}, error={}",
                    eventType, eventId, merchantId, kafkaOriginalTopic, partition, offset, exceptionMessage);

            // Store dead letter event for analysis
            String dlqRecordId = deadLetterQueueService.storeDlqEvent(
                event,
                eventType,
                eventId,
                merchantId,
                kafkaOriginalTopic != null ? kafkaOriginalTopic : originalTopic,
                exceptionMessage,
                stackTrace,
                Instant.now()
            );

            // Categorize the failure
            String failureCategory = categorizeFailure(exceptionMessage, stackTrace);

            // Send alert for critical events
            if (shouldAlert(eventType, failureCategory, merchantId)) {
                sendAlert(eventType, eventId, merchantId, failureCategory, exceptionMessage, dlqRecordId);
            }

            // Track metrics
            dlqCounter.increment(
                "event_type", eventType,
                "original_topic", kafkaOriginalTopic != null ? kafkaOriginalTopic : "unknown",
                "failure_category", failureCategory
            );

            // Check for patterns that might indicate system issues
            checkForSystemIssues(eventType, failureCategory, merchantId);

            log.info("Dead letter event processed and stored: dlqRecordId={}, eventType={}, category={}",
                    dlqRecordId, eventType, failureCategory);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process dead letter event: {}", e.getMessage(), e);

            // Still acknowledge to avoid infinite loops in DLQ processing
            acknowledgment.acknowledge();

        } finally {
            sample.stop(processingTimer);
        }
    }

    private String extractEventId(Object event) {
        if (event instanceof DomainEvent) {
            return ((DomainEvent) event).getEventId();
        }

        // Try to extract ID using reflection for other event types
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

        // Try to extract merchant ID using reflection
        try {
            var method = event.getClass().getMethod("getMerchantId");
            return (String) method.invoke(event);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String categorizeFailure(String exceptionMessage, String stackTrace) {
        if (exceptionMessage == null) {
            return "unknown";
        }

        String message = exceptionMessage.toLowerCase();

        if (message.contains("timeout") || message.contains("read timed out")) {
            return "timeout";
        } else if (message.contains("connection") || message.contains("network")) {
            return "network";
        } else if (message.contains("serialization") || message.contains("deserialization")) {
            return "serialization";
        } else if (message.contains("validation") || message.contains("constraint")) {
            return "validation";
        } else if (message.contains("database") || message.contains("sql")) {
            return "database";
        } else if (message.contains("null") || message.contains("npe")) {
            return "null_pointer";
        } else if (message.contains("authentication") || message.contains("authorization")) {
            return "auth";
        } else if (message.contains("rate") || message.contains("limit")) {
            return "rate_limit";
        } else {
            return "application";
        }
    }

    private boolean shouldAlert(String eventType, String failureCategory, String merchantId) {
        // Alert on critical event types
        if (eventType.contains("PaymentCompleted") || eventType.contains("SettlementCompleted")) {
            return true;
        }

        // Alert on system-wide issues
        if ("database".equals(failureCategory) || "network".equals(failureCategory)) {
            return true;
        }

        // Alert on validation errors for specific merchants (might indicate integration issues)
        if ("validation".equals(failureCategory) && merchantId != null && !merchantId.equals("unknown")) {
            return true;
        }

        return false;
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

            alertingService.sendDlqAlert(
                "Dead Letter Queue Alert",
                String.format("Event %s (%s) failed processing and was sent to DLQ", eventType, eventId),
                alertData
            );

            alertCounter.increment(
                "event_type", eventType,
                "failure_category", failureCategory
            );

            log.info("Alert sent for DLQ event: eventType={}, category={}, dlqRecordId={}",
                    eventType, failureCategory, dlqRecordId);

        } catch (Exception e) {
            log.error("Failed to send DLQ alert: {}", e.getMessage(), e);
        }
    }

    private void checkForSystemIssues(String eventType, String failureCategory, String merchantId) {
        try {
            // Check if we're seeing a spike in similar failures
            long recentSimilarFailures = deadLetterQueueService.countRecentFailures(
                failureCategory,
                java.time.Duration.ofMinutes(15)
            );

            if (recentSimilarFailures > 10) { // Threshold for system-wide issue
                alertingService.sendSystemAlert(
                    "High DLQ Volume Alert",
                    String.format("High volume of %s failures detected: %d events in last 15 minutes",
                            failureCategory, recentSimilarFailures),
                    Map.of(
                        "failure_category", failureCategory,
                        "failure_count", recentSimilarFailures,
                        "time_window", "15_minutes"
                    )
                );

                log.warn("System issue detected: {} failures in last 15 minutes for category {}",
                        recentSimilarFailures, failureCategory);
            }

        } catch (Exception e) {
            log.error("Failed to check for system issues: {}", e.getMessage(), e);
        }
    }
}