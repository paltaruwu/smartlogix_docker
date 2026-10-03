package com.storechain.rabbitadmin.service;

import com.storechain.rabbitadmin.dto.BindingRequest;
import com.storechain.rabbitadmin.dto.ExchangeRequest;
import com.storechain.rabbitadmin.dto.QueueRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
public class RabbitAdminService {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public Map<String, Object> createQueue(QueueRequest request) {
        Queue queue;

        if (request.isWithDlq()) {
            String dlqName = request.getName() + ".dlq";
            Queue dlq = QueueBuilder.durable(dlqName).build();
            rabbitAdmin.declareQueue(dlq);
            log.info("DLQ creada: {}", dlqName);

            queue = QueueBuilder.durable(request.getName())
                    .withArgument("x-dead-letter-exchange", "")
                    .withArgument("x-dead-letter-routing-key", dlqName)
                    .build();
        } else {
            queue = QueueBuilder.durable(request.getName()).build();
        }

        String queueName = rabbitAdmin.declareQueue(queue);
        log.info("Cola creada: {}", queueName);

        Map<String, Object> result = new HashMap<>();
        result.put("queue", queueName);
        result.put("durable", request.isDurable());
        result.put("dlq", request.isWithDlq() ? request.getName() + ".dlq" : null);
        return result;
    }

    public Map<String, Object> getQueueInfo(String name) {
        Properties props = rabbitAdmin.getQueueProperties(name);
        if (props == null) {
            return null;
        }
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("messageCount", props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT));
        info.put("consumerCount", props.get(RabbitAdmin.QUEUE_CONSUMER_COUNT));
        return info;
    }

    public void deleteQueue(String name) {
        rabbitAdmin.deleteQueue(name);
        log.info("Cola eliminada: {}", name);
    }

    public Map<String, Object> createExchange(ExchangeRequest request) {
        Exchange exchange = switch (request.getType().toLowerCase()) {
            case "topic" -> new TopicExchange(request.getName(), request.isDurable(), false);
            case "fanout" -> new FanoutExchange(request.getName(), request.isDurable(), false);
            case "headers" -> new HeadersExchange(request.getName(), request.isDurable(), false);
            default -> new DirectExchange(request.getName(), request.isDurable(), false);
        };

        rabbitAdmin.declareExchange(exchange);
        log.info("Exchange creado: {} (tipo: {})", request.getName(), request.getType());

        Map<String, Object> result = new HashMap<>();
        result.put("name", request.getName());
        result.put("type", request.getType());
        result.put("durable", request.isDurable());
        return result;
    }

    public Map<String, Object> createBinding(BindingRequest request) {
        rabbitAdmin.declareBinding(toBinding(request));
        log.info("Binding creado: {} -> {} con key {}", request.getQueueName(),
                request.getExchangeName(), request.getRoutingKey());

        Map<String, Object> result = new HashMap<>();
        result.put("queue", request.getQueueName());
        result.put("exchange", request.getExchangeName());
        result.put("routingKey", request.getRoutingKey());
        return result;
    }

    public void deleteBinding(BindingRequest request) {
        rabbitAdmin.removeBinding(toBinding(request));
        log.info("Binding eliminado: {} -> {} con key {}", request.getQueueName(),
                request.getExchangeName(), request.getRoutingKey());
    }

    public void deleteExchange(String name) {
        rabbitAdmin.deleteExchange(name);
        log.info("Exchange eliminado: {}", name);
    }

    // Binding genérico: funciona con cualquier tipo de exchange (direct, topic, fanout...)
    private Binding toBinding(BindingRequest request) {
        return new Binding(request.getQueueName(), Binding.DestinationType.QUEUE,
                request.getExchangeName(), request.getRoutingKey(), null);
    }

    public void publishMessage(String exchange, String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        log.info("Mensaje publicado a exchange={}, routingKey={}", exchange, routingKey);
    }
}
