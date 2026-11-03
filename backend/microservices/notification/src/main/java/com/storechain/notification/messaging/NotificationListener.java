package com.storechain.notification.messaging;

import com.rabbitmq.client.Channel;
import com.storechain.notification.exception.BusinessRuleException;
import com.storechain.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class NotificationListener {

    @Autowired
    private NotificationService notificationService;

    @Value("${rabbitmq.retry.max-attempts}")
    private int maxAttempts;

    @Value("${rabbitmq.retry.backoff-ms}")
    private long backoffMs;

    @RabbitListener(queues = "${rabbitmq.queue.notification}")
    public void onEvent(@Payload Map<String, Object> payload, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                        @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("[NOTIFICATION] Evento {} (intento {}/{})", routingKey, attempt, maxAttempts);
                notificationService.handleEvent(routingKey, payload);
                channel.basicAck(deliveryTag, false);
                return;
            } catch (BusinessRuleException e) {
                // Error no recuperable: reintentar no sirve, va directo a la DLQ
                log.error("[NOTIFICATION] Error de negocio, mensaje enviado a DLQ sin reintentos: {} | payload={}",
                        e.getMessage(), payload);
                nack(channel, deliveryTag);
                return;
            } catch (Exception e) {
                log.warn("[NOTIFICATION] Error recuperable en intento {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("[NOTIFICATION] Reintentos agotados, mensaje enviado a DLQ | payload={}", payload);
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
            log.error("[NOTIFICATION] Error enviando NACK: {}", ioEx.getMessage());
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
