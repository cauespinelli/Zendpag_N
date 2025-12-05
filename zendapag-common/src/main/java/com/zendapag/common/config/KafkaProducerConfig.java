package com.zendapag.common.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Configuration
@ConditionalOnProperty(name = "spring.kafka.producer.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaProducerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private int retries;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public SyncKafkaPublisher syncKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new SyncKafkaPublisher(kafkaTemplate);
    }

    @Bean
    public AsyncKafkaPublisher asyncKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new AsyncKafkaPublisher(kafkaTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.producer.transaction-id-prefix")
    public TransactionalKafkaPublisher transactionalKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new TransactionalKafkaPublisher(kafkaTemplate);
    }

    public static class SyncKafkaPublisher {
        private final KafkaTemplate<String, Object> kafkaTemplate;

        public SyncKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        public SendResult<String, Object> send(String topic, String key, Object message)
                throws InterruptedException, ExecutionException, TimeoutException {
            return send(topic, key, message, 30);
        }

        public SendResult<String, Object> send(String topic, String key, Object message, long timeoutSeconds)
                throws InterruptedException, ExecutionException, TimeoutException {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        }
    }

    public static class AsyncKafkaPublisher {
        private static final Logger log = LoggerFactory.getLogger(AsyncKafkaPublisher.class);
        private final KafkaTemplate<String, Object> kafkaTemplate;

        public AsyncKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        public void send(String topic, String key, Object message) {
            send(topic, key, message, null, null);
        }

        public void send(String topic, String key, Object message,
                        Runnable onSuccess,
                        Consumer<Exception> onFailure) {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send message: topic={}, key={}", topic, key, ex);
                    if (onFailure != null) {
                        onFailure.accept(ex instanceof Exception ? (Exception) ex : new RuntimeException(ex));
                    }
                } else {
                    log.debug("Message sent: topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }
            });
        }
    }

    public static class TransactionalKafkaPublisher {
        private final KafkaTemplate<String, Object> kafkaTemplate;

        public TransactionalKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        public void executeInTransaction(Runnable operation) {
            kafkaTemplate.executeInTransaction(t -> {
                operation.run();
                return true;
            });
        }

        public <T> T executeInTransaction(java.util.function.Supplier<T> operation) {
            return kafkaTemplate.executeInTransaction(t -> operation.get());
        }
    }
}
