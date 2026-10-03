package com.storechain.shipment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.topic}")
    private String topicExchangeName;

    @Value("${rabbitmq.queue.shipment.dispatch}")
    private String shipmentDispatchQueue;

    @Value("${rabbitmq.queue.shipment.dispatch.dlq}")
    private String shipmentDispatchDlq;

    @Value("${rabbitmq.routing.key.shipment.dispatch}")
    private String shipmentDispatchKey;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(topicExchangeName, true, false);
    }

    @Bean
    public Queue shipmentDispatchDlq() {
        return QueueBuilder.durable(shipmentDispatchDlq).build();
    }

    @Bean
    public Queue shipmentDispatchQueue() {
        return QueueBuilder.durable(shipmentDispatchQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", shipmentDispatchDlq)
                .build();
    }

    @Bean
    public Binding shipmentDispatchBinding(
            @Qualifier("shipmentDispatchQueue") Queue shipmentDispatchQueue,
            TopicExchange topicExchange) {
        return BindingBuilder.bind(shipmentDispatchQueue).to(topicExchange).with(shipmentDispatchKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
