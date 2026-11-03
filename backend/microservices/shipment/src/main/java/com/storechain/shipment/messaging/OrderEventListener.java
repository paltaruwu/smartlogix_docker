package com.storechain.shipment.messaging;

import com.storechain.shipment.dto.OrderEvent;
import com.storechain.shipment.service.ShipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consume order-events (grupo shipment-group) y crea el envío cuando un pedido es aprobado.
 * Es un grupo distinto al de inventory: cada grupo recibe todos los eventos del tópico.
 * Solo valida y delega: la lógica vive en ShipmentService.
 * Si lanza una excepción, el error handler de KafkaConfig reintenta y luego envía el evento al DLT.
 */
@Slf4j
@Component
public class OrderEventListener {

    @Autowired
    private ShipmentService shipmentService;

    @KafkaListener(topics = "${kafka.topic.order-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderEvent(@Payload OrderEvent event,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("[KAFKA] Evento {} recibido (partición {}, offset {}): pedido {}",
                event.getEventType(), partition, offset, event.getOrderNumber());

        if (event.getEventType() == null || event.getOrderId() == null) {
            throw new IllegalArgumentException("Evento de pedido inválido: falta eventType u orderId");
        }

        // Solo un pedido aprobado se despacha; creado y rechazado no requieren acción en shipment
        if ("ORDER_APPROVED".equals(event.getEventType())) {
            shipmentService.createForOrder(event.getOrderId(), event.getClient(), event.getAddress());
        }
    }
}
