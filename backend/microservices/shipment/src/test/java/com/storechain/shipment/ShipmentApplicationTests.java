package com.storechain.shipment;

import com.storechain.shipment.entities.Shipment;
import com.storechain.shipment.exception.BusinessRuleException;
import com.storechain.shipment.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Los tests no dependen de MySQL, Eureka ni Kafka: usan H2 en memoria y no arrancan los consumidores
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:shipmentdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.kafka.admin.auto-create=false",
		"spring.kafka.listener.auto-startup=false"
})
class ShipmentApplicationTests {

	@Autowired
	private ShipmentService shipmentService;

	@Test
	void contextLoads() {
	}

	@Test
	void createsShipmentForApprovedOrderOnlyOnce() {
		Shipment first = shipmentService.createForOrder(500L, "Cliente", "Calle 1");
		assertNotNull(first);
		assertEquals("ENVIADO", first.getStatus());

		// Kafka entrega al menos una vez: el mismo pedido no debe generar un segundo envío
		assertNull(shipmentService.createForOrder(500L, "Cliente", "Calle 1"));
	}

	@Test
	void rejectsShipmentWithoutOrderId() {
		assertThrows(BusinessRuleException.class, () -> shipmentService.create(new Shipment()));
	}

}
