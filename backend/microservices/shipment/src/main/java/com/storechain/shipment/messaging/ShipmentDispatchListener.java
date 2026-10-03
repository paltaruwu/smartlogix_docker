package com.storechain.shipment.messaging;

import com.rabbitmq.client.Channel;
import com.storechain.shipment.dto.ShipmentDispatchMessage;
import com.storechain.shipment.entities.Shipment;
import com.storechain.shipment.exception.BusinessRuleException;
import com.storechain.shipment.service.ShipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ShipmentDispatchListener {

    @Autowired
    private ShipmentService shipmentService;

    @Value("${rabbitmq.retry.max-attempts}")
    private int maxAttempts;

    @Value("${rabbitmq.retry.backoff-ms}")
    private long backoffMs;

    @RabbitListener(queues = "${rabbitmq.queue.shipment.dispatch}")
    public void onDispatch(@Payload ShipmentDispatchMessage message, Channel channel,
                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("[DISPATCH] Creando envío (intento {}/{}) para orden: {} - cliente: {}",
                        attempt, maxAttempts, message.getOrderNumber(), message.getClient());

                Shipment shipment = new Shipment();
                shipment.setOrderId(message.getOrderId());
                shipment.setClient(message.getClient());
                shipment.setAddress(message.getAddress());

                shipmentService.create(shipment);
                channel.basicAck(deliveryTag, false);
                log.info("[DISPATCH] Envío creado exitosamente para orden: {}", message.getOrderNumber());
                return;
            } catch (BusinessRuleException e) {
                // Error no recuperable: reintentar no sirve, va directo a la DLQ
                log.error("[DISPATCH] Error de negocio, mensaje enviado a DLQ sin reintentos: {} | payload={}",
                        e.getMessage(), message);
                nack(channel, deliveryTag);
                return;
            } catch (Exception e) {
                log.warn("[DISPATCH] Error recuperable en intento {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("[DISPATCH] Reintentos agotados, mensaje enviado a DLQ | payload={}", message);
                    nack(channel, deliveryTag);
                    return;
                }
                pause();
            }
        }
    }

    private void nack(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException ioEx) {
            log.error("[DISPATCH] Error enviando NACK: {}", ioEx.getMessage());
        }
    }

    private void pause() {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
