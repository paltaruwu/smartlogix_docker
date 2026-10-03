package com.storechain.order.messaging;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OrderNotificationListener {

    @RabbitListener(queues = "${rabbitmq.queue.order.notification}")
    public void onOrderNotification(Message message, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            log.info("[NOTIFICACIÓN] Nueva orden recibida: {}", body);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[NOTIFICACIÓN] Error procesando mensaje, enviado a DLQ: {} | payload={}", e.getMessage(), body);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioEx) {
                log.error("[NOTIFICACIÓN] Error enviando NACK: {}", ioEx.getMessage());
            }
        }
    }
}
