package com.storechain.technician.config;

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

    @Value("${rabbitmq.queue.payment.result}")
    private String paymentResultQueue;

    @Value("${rabbitmq.queue.payment.result.dlq}")
    private String paymentResultDlq;

    @Value("${rabbitmq.routing.key.payment.result}")
    private String paymentResultKey;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(topicExchangeName, true, false);
    }

    @Bean
    public Queue paymentResultDlq() {
        return QueueBuilder.durable(paymentResultDlq).build();
    }

    @Bean
    public Queue paymentResultQueue() {
        return QueueBuilder.durable(paymentResultQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", paymentResultDlq)
                .build();
    }

    @Bean
    public Binding paymentResultBinding(
            @Qualifier("paymentResultQueue") Queue paymentResultQueue,
            TopicExchange topicExchange) {
        return BindingBuilder.bind(paymentResultQueue).to(topicExchange).with(paymentResultKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
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
