package com.storechain.technician.messaging;

import com.storechain.technician.dto.PaymentRequestMessage;
import com.storechain.technician.dto.ServiceEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TechnicianEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.direct}")
    private String directExchange;

    @Value("${rabbitmq.exchange.topic}")
    private String topicExchange;

    @Value("${rabbitmq.routing.key.payment.request}")
    private String paymentRequestKey;

    @Value("${rabbitmq.routing.key.event.prefix}")
    private String eventPrefix;

    public void publishPaymentRequest(PaymentRequestMessage msg) {
        rabbitTemplate.convertAndSend(directExchange, paymentRequestKey, msg);
        log.info("[TECHNICIAN] Pago solicitado para solicitud {} por {}", msg.getReferenceId(), msg.getAmount());
    }

    public void publishServiceEvent(ServiceEventMessage msg) {
        try {
            rabbitTemplate.convertAndSend(topicExchange, eventPrefix + "." + msg.getEvent(), msg);
            log.info("[TECHNICIAN] Evento {} publicado para solicitud {}", msg.getEvent(), msg.getRequestId());
        } catch (Exception e) {
            log.error("[TECHNICIAN] Error publicando evento {}: {}", msg.getEvent(), e.getMessage());
        }
    }
}
