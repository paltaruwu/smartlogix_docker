package com.storechain.inventory.messaging;

import com.rabbitmq.client.Channel;
import com.storechain.inventory.dto.RestockMessage;
import com.storechain.inventory.exception.BusinessRuleException;
import com.storechain.inventory.service.InventoryService;
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
public class InventoryRestockListener {

    @Autowired
    private InventoryService inventoryService;

    @Value("${rabbitmq.retry.max-attempts}")
    private int maxAttempts;

    @Value("${rabbitmq.retry.backoff-ms}")
    private long backoffMs;

    @RabbitListener(queues = "${rabbitmq.queue.inventory.restock}")
    public void onRestock(@Payload RestockMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("[RESTOCK] Reabastecimiento vía RabbitMQ (intento {}/{}): producto={}, cantidad={}, motivo={}",
                        attempt, maxAttempts, message.getProductId(), message.getQuantity(), message.getReason());
                inventoryService.addStockManual(message.getProductId(), message.getQuantity(), message.getReason());
                channel.basicAck(deliveryTag, false);
                log.info("[RESTOCK] Reabastecimiento completado para producto: {}", message.getProductId());
                return;
            } catch (BusinessRuleException e) {
                // Error no recuperable: reintentar no sirve, va directo a la DLQ
                log.error("[RESTOCK] Error de negocio, mensaje enviado a DLQ sin reintentos: {} | payload={}",
                        e.getMessage(), message);
                nack(channel, deliveryTag);
                return;
            } catch (Exception e) {
                log.warn("[RESTOCK] Error recuperable en intento {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("[RESTOCK] Reintentos agotados, mensaje enviado a DLQ | payload={}", message);
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
            log.error("[RESTOCK] Error enviando NACK: {}", ioEx.getMessage());
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
