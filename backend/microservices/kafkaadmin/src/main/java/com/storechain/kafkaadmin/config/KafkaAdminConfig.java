package com.storechain.kafkaadmin.config;

import org.apache.kafka.clients.admin.Admin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaAdminConfig {

    // Cliente administrador de Kafka, con la conexión definida en spring.kafka.bootstrap-servers
    @Bean(destroyMethod = "close")
    public Admin adminClient(KafkaAdmin kafkaAdmin) {
        return Admin.create(kafkaAdmin.getConfigurationProperties());
    }
}
