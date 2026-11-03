package com.storechain.inventory.service;

import com.storechain.inventory.dto.OrderEvent;
import com.storechain.inventory.dto.OrderEventItem;
import com.storechain.inventory.entities.LowStockAlert;
import com.storechain.inventory.entities.Product;
import com.storechain.inventory.repository.LowStockAlertRepository;
import com.storechain.inventory.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lógica de negocio de las alertas de bajo stock. No sabe nada de Kafka: el consumidor
 * (OrderEventListener) solo le entrega el evento ya validado.
 */
@Slf4j
@Service
public class LowStockService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LowStockAlertRepository alertRepository;

    @Value("${inventory.low-stock-threshold:5}")
    private int threshold;

    /** Revisa el stock de cada producto del pedido (order ya lo descontó antes de publicar el evento). */
    public void evaluateOrder(OrderEvent event) {
        for (OrderEventItem item : event.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null) {
                log.warn("[LOW-STOCK] Producto {} del pedido {} no existe, se ignora", item.getProductId(),
                        event.getOrderNumber());
                continue;
            }

            if (product.getStock() <= threshold) {
                LowStockAlert alert = new LowStockAlert();
                alert.setProductId(product.getId());
                alert.setProductTitle(product.getTitle());
                alert.setStock(product.getStock());
                alert.setThreshold(threshold);
                alert.setOrderNumber(event.getOrderNumber());
                alertRepository.save(alert);
                log.warn("[LOW-STOCK] ALERTA: '{}' quedó con stock {} (umbral {}) tras el pedido {}",
                        product.getTitle(), product.getStock(), threshold, event.getOrderNumber());
            }
        }
    }

    public List<LowStockAlert> listAlerts() {
        return alertRepository.findAllByOrderByIdDesc();
    }
}
