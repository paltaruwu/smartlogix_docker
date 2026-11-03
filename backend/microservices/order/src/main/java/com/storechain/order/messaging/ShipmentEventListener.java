package com.storechain.order.messaging;

import com.storechain.order.dto.ShipmentEvent;
import com.storechain.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consume shipment-events (grupo order-group) y deja el estado del envío en el pedido.
 * Solo valida y delega: la lógica de negocio vive en OrderService.
 * Si lanza una excepción, el error handler de KafkaConfig reintenta y luego envía el evento al DLT.
 */
@Slf4j
@Component
public class ShipmentEventListener {

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "${kafka.topic.shipment-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onShipmentEvent(@Payload ShipmentEvent event,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("[KAFKA] Evento {} recibido (partición {}, offset {}): pedido {} -> envío {}",
                event.getEventType(), partition, offset, event.getOrderId(), event.getStatus());

        if (event.getOrderId() == null || event.getStatus() == null || event.getStatus().isBlank()) {
            throw new IllegalArgumentException("Evento de envío inválido: falta orderId o status");
        }

        orderService.registerShipmentStatus(event.getOrderId(), event.getStatus());
    }
}
