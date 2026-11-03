package com.storechain.order.config;

import com.storechain.order.exception.BusinessRuleException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuración Kafka del microservicio order: declara los tópicos que produce y el manejo de errores
 * de los consumidores (reintentos + Dead Letter Topic). Los parámetros del cliente están en application.properties.
 */
@Slf4j
@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.order-events}")
    private String orderEventsTopic;

    @Value("${kafka.topic.shipment-events}")
    private String shipmentEventsTopic;

    @Value("${kafka.retry.max-attempts}")
    private long maxAttempts;

    @Value("${kafka.retry.backoff-ms}")
    private long backoffMs;

    // Tópico que order produce: stream de eventos, se conserva 7 días
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(orderEventsTopic)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }

    // Dead Letter Topic del tópico que order consume (shipment-events)
    @Bean
    public NewTopic shipmentEventsDlt() {
        return TopicBuilder.name(shipmentEventsTopic + ".DLT")
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG, "1209600000")
                .build();
    }

    /**
     * Si un evento falla se reintenta; agotados los reintentos (o si el error no es recuperable)
     * va al tópico <original>.DLT y queda registrado en el log.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> jsonTemplate,
                                                 ProducerFactory<Object, Object> producerFactory) {
        // Un evento que no se pudo deserializar llega como bytes: necesita un template con ByteArraySerializer
        KafkaTemplate<Object, Object> bytesTemplate = new KafkaTemplate<>(producerFactory,
                Map.of(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class));

        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(templates,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(backoffMs, maxAttempts));
        // Datos inválidos o reglas de negocio no se arreglan reintentando: directo al DLT
        handler.addNotRetryableExceptions(IllegalArgumentException.class, BusinessRuleException.class);
        handler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                log.warn("[KAFKA] Reintento {} para {}-{}@{}: {}", deliveryAttempt, record.topic(),
                        record.partition(), record.offset(), ex.getMessage());
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.error("[KAFKA] Evento enviado a {}.DLT: {}-{}@{} | causa: {} | valor: {}", record.topic(),
                        record.topic(), record.partition(), record.offset(), rootMessage(ex), record.value());
            }
        });
        return handler;
    }

    private static String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
