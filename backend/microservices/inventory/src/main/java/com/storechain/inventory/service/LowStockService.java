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

import java.time.LocalDateTime;
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

    /**
     * Cierra las alertas abiertas de un producto cuando, tras una reposición, su stock supera el umbral.
     * Es idempotente: Kafka entrega al menos una vez, y repetir el evento no cambia nada.
     */
    public int resolveAlerts(Long productId, Double stockAfter) {
        if (stockAfter <= threshold) {
            return 0;
        }

        List<LowStockAlert> open = alertRepository.findByProductId(productId).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getResolved()))
                .toList();

        open.forEach(a -> {
            a.setResolved(true);
            a.setResolvedAt(LocalDateTime.now());
        });
        alertRepository.saveAll(open);

        if (!open.isEmpty()) {
            log.info("[LOW-STOCK] {} alerta(s) del producto {} resueltas: stock repuesto a {} (umbral {})",
                    open.size(), productId, stockAfter, threshold);
        }
        return open.size();
    }

    public List<LowStockAlert> listAlerts() {
        return alertRepository.findAllByOrderByIdDesc();
    }
}
