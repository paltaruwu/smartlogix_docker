package com.storechain.order;

import com.storechain.order.entities.Order;
import com.storechain.order.exception.BusinessRuleException;
import com.storechain.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;

// Los tests no dependen de MySQL, Eureka ni Kafka: usan H2 en memoria y no arrancan los consumidores
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:orderdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.kafka.admin.auto-create=false",
		"spring.kafka.listener.auto-startup=false"
})
class OrderApplicationTests {

	@org.springframework.beans.factory.annotation.Autowired
	private OrderService orderService;

	@Test
	void contextLoads() {
	}

	@Test
	void rejectsOrderWithoutProducts() {
		Order order = new Order();
		order.setClient("Cliente");
		order.setAddress("Calle 1");
		order.setDetails(new ArrayList<>());

		assertThrows(BusinessRuleException.class, () -> orderService.createOrder(order));
	}

	@Test
	void rejectsUnknownStatus() {
		assertThrows(BusinessRuleException.class, () -> orderService.changeStatus(999999L, "APROBADO"));
	}

}
