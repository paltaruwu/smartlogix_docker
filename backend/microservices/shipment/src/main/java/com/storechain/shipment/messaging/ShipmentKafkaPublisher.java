package com.storechain.shipment.messaging;

import com.storechain.shipment.dto.ShipmentEvent;
import com.storechain.shipment.entities.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica el estado de los envíos en shipment-events (clave = id del pedido).
 * Un fallo de Kafka solo se registra: nunca debe impedir crear o actualizar un envío.
 */
@Slf4j
@Component
public class ShipmentKafkaPublisher {

    public static final String SHIPMENT_CREATED = "SHIPMENT_CREATED";
    public static final String SHIPMENT_STATUS_CHANGED = "SHIPMENT_STATUS_CHANGED";

    @Autowired
    private KafkaTemplate<String, ShipmentEvent> kafkaTemplate;

    @Value("${kafka.topic.shipment-events}")
    private String topic;

    public void publish(String eventType, Shipment shipment) {
        ShipmentEvent event = new ShipmentEvent(
                UUID.randomUUID().toString(),
                eventType,
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getStatus(),
                shipment.getCarrier(),
                shipment.getEstimatedDate(),
                Instant.now().toString());

        try {
            kafkaTemplate.send(topic, String.valueOf(shipment.getOrderId()), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KAFKA] No se pudo publicar {} del envío {}: {}", eventType, shipment.getId(),
                            ex.getMessage());
                } else {
                    log.info("[KAFKA] {} publicado: envío {} (pedido {}) -> {}-{}@{}", eventType, shipment.getId(),
                            shipment.getOrderId(), result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("[KAFKA] Error publicando {} del envío {}: {}", eventType, shipment.getId(), e.getMessage());
        }
    }
}
