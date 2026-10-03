package com.storechain.inventory.config;

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

    @Value("${rabbitmq.queue.inventory.restock}")
    private String inventoryRestockQueue;

    @Value("${rabbitmq.queue.inventory.restock.dlq}")
    private String inventoryRestockDlq;

    @Value("${rabbitmq.routing.key.inventory.restock}")
    private String inventoryRestockKey;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(topicExchangeName, true, false);
    }

    @Bean
    public Queue inventoryRestockDlq() {
        return QueueBuilder.durable(inventoryRestockDlq).build();
    }

    @Bean
    public Queue inventoryRestockQueue() {
        return QueueBuilder.durable(inventoryRestockQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", inventoryRestockDlq)
                .build();
    }

    @Bean
    public Binding inventoryRestockBinding(
            @Qualifier("inventoryRestockQueue") Queue inventoryRestockQueue,
            TopicExchange topicExchange) {
        return BindingBuilder.bind(inventoryRestockQueue).to(topicExchange).with(inventoryRestockKey);
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
