package com.storechain.technician.messaging;

import com.rabbitmq.client.Channel;
import com.storechain.technician.dto.PaymentResultMessage;
import com.storechain.technician.exception.BusinessRuleException;
import com.storechain.technician.service.TechnicianService;
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
public class PaymentResultListener {

    @Autowired
    private TechnicianService technicianService;

    @Value("${rabbitmq.retry.max-attempts}")
    private int maxAttempts;

    @Value("${rabbitmq.retry.backoff-ms}")
    private long backoffMs;

    @RabbitListener(queues = "${rabbitmq.queue.payment.result}")
    public void onPaymentResult(@Payload PaymentResultMessage message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("[PAYMENT-RESULT] Solicitud {} -> {} (intento {}/{})",
                        message.getReferenceId(), message.getStatus(), attempt, maxAttempts);
                technicianService.applyPaymentResult(message);
                channel.basicAck(deliveryTag, false);
                return;
            } catch (BusinessRuleException e) {
                // Error no recuperable: reintentar no sirve, va directo a la DLQ
                log.error("[PAYMENT-RESULT] Error de negocio, mensaje enviado a DLQ sin reintentos: {} | payload={}",
                        e.getMessage(), message);
                nack(channel, deliveryTag);
                return;
            } catch (Exception e) {
                log.warn("[PAYMENT-RESULT] Error recuperable en intento {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("[PAYMENT-RESULT] Reintentos agotados, mensaje enviado a DLQ | payload={}", message);
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
            log.error("[PAYMENT-RESULT] Error enviando NACK: {}", ioEx.getMessage());
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
