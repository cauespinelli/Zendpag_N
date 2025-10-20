package com.zendapag.worker.consumer;

import com.zendapag.worker.dto.TransactionEvent;
import com.zendapag.worker.service.TransactionProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {

    private final TransactionProcessorService transactionProcessorService;

    @KafkaListener(
        topics = "${app.kafka.topics.transaction-events}",
        groupId = "${app.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processTransactionEvent(@Payload TransactionEvent event,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      Acknowledgment acknowledgment) {
        log.info("Received transaction event: {} from topic: {}, partition: {}, offset: {}",
                event.getTransactionId(), topic, partition, offset);

        try {
            transactionProcessorService.processTransaction(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed transaction: {}", event.getTransactionId());
        } catch (Exception ex) {
            log.error("Error processing transaction: {}", event.getTransactionId(), ex);
            // Implement retry logic or dead letter queue handling here
            acknowledgment.acknowledge(); // For now, acknowledge to prevent infinite retries
        }
    }

    @KafkaListener(
        topics = "${app.kafka.topics.pix-webhook}",
        groupId = "${app.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processPixWebhook(@Payload String webhookPayload,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                Acknowledgment acknowledgment) {
        log.info("Received PIX webhook from topic: {}", topic);

        try {
            transactionProcessorService.processPixWebhook(webhookPayload);
            acknowledgment.acknowledge();
            log.info("Successfully processed PIX webhook");
        } catch (Exception ex) {
            log.error("Error processing PIX webhook", ex);
            acknowledgment.acknowledge();
        }
    }
}