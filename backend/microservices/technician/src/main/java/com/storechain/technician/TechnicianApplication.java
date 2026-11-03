package com.storechain.technician;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
		info = @Info(
				title = "API de Technician - SmartLogix",
				version = "1.0",
				description = "Catálogo de técnicos y contratación de servicios técnicos, con pago vía RabbitMQ"
		)
)
public class TechnicianApplication {

	public static void main(String[] args) {
		SpringApplication.run(TechnicianApplication.class, args);
	}

}
