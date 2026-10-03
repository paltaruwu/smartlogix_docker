package com.storechain.order.messaging;

import com.storechain.order.dto.OrderCreatedEvent;
import com.storechain.order.dto.ShipmentDispatchMessage;
import com.storechain.order.entities.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.direct}")
    private String directExchange;

    @Value("${rabbitmq.exchange.topic}")
    private String topicExchange;

    @Value("${rabbitmq.routing.key.order.notification}")
    private String orderNotificationKey;

    @Value("${rabbitmq.routing.key.shipment.dispatch}")
    private String shipmentDispatchKey;

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getClient(),
                order.getAddress(),
                order.getStatus()
        );
        try {
            rabbitTemplate.convertAndSend(directExchange, orderNotificationKey, event);
            log.info("Evento order.created publicado para orden: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Error publicando evento order.created: {}", e.getMessage());
        }
    }

    public void publishShipmentDispatch(Order order) {
        ShipmentDispatchMessage msg = new ShipmentDispatchMessage(
                order.getId(),
                order.getOrderNumber(),
                order.getClient(),
                order.getAddress()
        );
        try {
            rabbitTemplate.convertAndSend(topicExchange, shipmentDispatchKey, msg);
            log.info("Evento shipment.dispatch publicado para orden: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Error publicando evento shipment.dispatch: {}", e.getMessage());
        }
    }
}
