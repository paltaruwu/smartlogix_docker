package com.storechain.inventory;

import com.storechain.inventory.entities.LowStockAlert;
import com.storechain.inventory.exception.BusinessRuleException;
import com.storechain.inventory.repository.LowStockAlertRepository;
import com.storechain.inventory.service.InventoryService;
import com.storechain.inventory.service.LowStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Autowired
	private LowStockService lowStockService;

	@Autowired
	private LowStockAlertRepository alertRepository;

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

	@Test
	void restockAboveThresholdResolvesOpenAlerts() {
		LowStockAlert alert = new LowStockAlert();
		alert.setProductId(777L);
		alert.setProductTitle("Producto de prueba");
		alert.setStock(2.0);
		alert.setThreshold(5);
		alert.setOrderNumber("ORD-TEST");
		alertRepository.save(alert);

		// Una reposición que no supera el umbral (5) no cierra la alerta
		assertEquals(0, lowStockService.resolveAlerts(777L, 4.0));

		// Una que lo supera sí la cierra, y repetir el mismo evento no hace nada más
		assertEquals(1, lowStockService.resolveAlerts(777L, 20.0));
		assertEquals(0, lowStockService.resolveAlerts(777L, 20.0));

		assertTrue(alertRepository.findByProductId(777L).get(0).getResolved());
	}

}
