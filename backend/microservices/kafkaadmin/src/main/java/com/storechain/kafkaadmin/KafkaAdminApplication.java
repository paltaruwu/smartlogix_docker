package com.storechain.kafkaadmin;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
		info = @Info(
				title = "API de Kafka Admin - SmartLogix",
				version = "1.0",
				description = "Administración del clúster Kafka: tópicos, configuraciones, particiones y consumer groups"
		)
)
public class KafkaAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaAdminApplication.class, args);
	}

}
