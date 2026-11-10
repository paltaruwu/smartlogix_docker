package com.storechain.inventory.messaging;

import com.storechain.inventory.dto.StockEvent;
import com.storechain.inventory.entities.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publica los cambios de stock en el tópico inventory-events (clave = id del producto, así todos los
 * eventos de un mismo producto caen en la misma partición y se consumen en orden).
 * Un fallo de Kafka solo se registra: nunca debe impedir reponer el stock.
 */
@Slf4j
@Component
public class InventoryKafkaPublisher {

    public static final String STOCK_REPLENISHED = "STOCK_REPLENISHED";

    @Autowired
    private KafkaTemplate<String, StockEvent> kafkaTemplate;

    @Value("${kafka.topic.inventory-events}")
    private String topic;

    public void publishReplenished(Product product, int quantity, String reason) {
        StockEvent event = new StockEvent(
                UUID.randomUUID().toString(),
                STOCK_REPLENISHED,
                product.getId(),
                product.getTitle(),
                quantity,
                product.getStock(),
                reason,
                Instant.now().toString());

        try {
            kafkaTemplate.send(topic, String.valueOf(product.getId()), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KAFKA] No se pudo publicar {} del producto {}: {}", STOCK_REPLENISHED,
                            product.getId(), ex.getMessage());
                } else {
                    log.info("[KAFKA] {} publicado: '{}' +{} (stock {}) -> {}-{}@{}", STOCK_REPLENISHED,
                            product.getTitle(), quantity, product.getStock(), result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("[KAFKA] Error publicando {} del producto {}: {}", STOCK_REPLENISHED, product.getId(),
                    e.getMessage());
        }
    }
}
