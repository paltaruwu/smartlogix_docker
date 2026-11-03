package com.storechain.inventory.messaging;

import com.storechain.inventory.dto.OrderEvent;
import com.storechain.inventory.service.LowStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consume order-events (grupo inventory-group). Los 3 consumidores concurrentes se reparten las
 * 3 particiones del tópico; si se levantan más instancias del servicio, Kafka rebalancea entre ellas.
 * Solo valida y delega: la lógica vive en LowStockService.
 * Si lanza una excepción, el error handler de KafkaConfig reintenta y luego envía el evento al DLT.
 */
@Slf4j
@Component
public class OrderEventListener {

    @Autowired
    private LowStockService lowStockService;

    @KafkaListener(topics = "${kafka.topic.order-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderEvent(@Payload OrderEvent event,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("[KAFKA] Evento {} recibido (partición {}, offset {}): pedido {}",
                event.getEventType(), partition, offset, event.getOrderNumber());

        if (event.getEventType() == null || event.getOrderId() == null) {
            throw new IllegalArgumentException("Evento de pedido inválido: falta eventType u orderId");
        }

        // Solo un pedido nuevo cambia el stock; aprobado y rechazado no requieren acción en inventory
        if ("ORDER_CREATED".equals(event.getEventType())) {
            if (event.getItems() == null || event.getItems().isEmpty()) {
                throw new IllegalArgumentException("ORDER_CREATED sin productos: pedido " + event.getOrderNumber());
            }
            lowStockService.evaluateOrder(event);
        }
    }
}
