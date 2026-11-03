package com.storechain.inventory;

import com.storechain.inventory.exception.BusinessRuleException;
import com.storechain.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

// Los tests no dependen de MySQL, Eureka ni Kafka: usan H2 en memoria y no arrancan los consumidores
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:inventorydb;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.kafka.admin.auto-create=false",
		"spring.kafka.listener.auto-startup=false"
})
class InventoryApplicationTests {

	@Autowired
	private InventoryService inventoryService;

	@Test
	void contextLoads() {
	}

	@Test
	void rejectsStockBelowZero() {
		// El stock inicial de "Laptop Gamer" (id 1) es 10: restar 50 debe fallar
		assertThrows(BusinessRuleException.class, () -> inventoryService.updateStock(1L, -50));
	}

	@Test
	void rejectsManualRestockWithNonPositiveQuantity() {
		assertThrows(BusinessRuleException.class, () -> inventoryService.addStockManual(1L, 0, "prueba"));
	}

}
