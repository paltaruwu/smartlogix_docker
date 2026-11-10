package com.storechain.inventory.messaging;

import com.storechain.inventory.dto.StockEvent;
import com.storechain.inventory.service.LowStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consume inventory-events (grupo inventory-stock-group) y cierra las alertas de bajo stock de un
 * producto cuando la reposición lo deja otra vez sobre el umbral. Es un grupo distinto al que lee
 * order-events, y declara su propio tipo de mensaje (StockEvent).
 * Solo valida y delega: la lógica vive en LowStockService.
 * Si lanza una excepción, el error handler de KafkaConfig reintenta y luego envía el evento al DLT.
 */
@Slf4j
@Component
public class StockEventListener {

    @Autowired
    private LowStockService lowStockService;

    @KafkaListener(
            topics = "${kafka.topic.inventory-events}",
            groupId = "inventory-stock-group",
            properties = "spring.json.value.default.type=com.storechain.inventory.dto.StockEvent")
    public void onStockEvent(@Payload StockEvent event,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("[KAFKA] Evento {} recibido (partición {}, offset {}): producto {}",
                event.getEventType(), partition, offset, event.getProductId());

        if (event.getEventType() == null || event.getProductId() == null || event.getStockAfter() == null) {
            throw new IllegalArgumentException("Evento de stock inválido: falta eventType, productId o stockAfter");
        }

        if (InventoryKafkaPublisher.STOCK_REPLENISHED.equals(event.getEventType())) {
            lowStockService.resolveAlerts(event.getProductId(), event.getStockAfter());
        }
    }
}
