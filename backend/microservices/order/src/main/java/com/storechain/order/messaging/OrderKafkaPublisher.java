package com.storechain.order.messaging;

import com.storechain.order.dto.OrderEvent;
import com.storechain.order.dto.OrderEventItem;
import com.storechain.order.entities.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Publica los eventos del pedido en el tópico order-events (clave = id del pedido, así todos los
 * eventos de un mismo pedido caen en la misma partición y se consumen en orden).
 * Un fallo de Kafka solo se registra: nunca debe impedir crear o aprobar un pedido.
 */
@Slf4j
@Component
public class OrderKafkaPublisher {

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_APPROVED = "ORDER_APPROVED";
    public static final String ORDER_REJECTED = "ORDER_REJECTED";

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${kafka.topic.order-events}")
    private String topic;

    /** Para eventos de los que depende otro servicio (ORDER_APPROVED): espera la confirmación de Kafka o lanza error. */
    public void publishAndWait(String eventType, Order order) throws Exception {
        OrderEvent event = buildEvent(eventType, order);
        var result = kafkaTemplate.send(topic, String.valueOf(order.getId()), event).get(10, TimeUnit.SECONDS);
        log.info("[KAFKA] {} publicado: pedido {} -> {}-{}@{}", eventType, order.getOrderNumber(),
                result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    public void publish(String eventType, Order order) {
        OrderEvent event = buildEvent(eventType, order);

        try {
            kafkaTemplate.send(topic, String.valueOf(order.getId()), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KAFKA] No se pudo publicar {} del pedido {}: {}", eventType, order.getOrderNumber(),
                            ex.getMessage());
                } else {
                    log.info("[KAFKA] {} publicado: pedido {} -> {}-{}@{}", eventType, order.getOrderNumber(),
                            result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("[KAFKA] Error publicando {} del pedido {}: {}", eventType, order.getOrderNumber(), e.getMessage());
        }
    }

    private OrderEvent buildEvent(String eventType, Order order) {
        List<OrderEventItem> items = order.getDetails() == null ? List.of() : order.getDetails().stream()
                .map(d -> new OrderEventItem(d.getProductId(), d.getQuantity()))
                .toList();

        return new OrderEvent(
                UUID.randomUUID().toString(),
                eventType,
                order.getId(),
                order.getOrderNumber(),
                order.getClient(),
                order.getAddress(),
                order.getStatus(),
                items,
                Instant.now().toString());
    }
}
