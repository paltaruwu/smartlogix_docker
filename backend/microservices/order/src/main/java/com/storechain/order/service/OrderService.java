package com.storechain.order.service;

import com.storechain.order.entities.*;
import com.storechain.order.exception.BusinessRuleException;
import com.storechain.order.messaging.OrderKafkaPublisher;
import com.storechain.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private OrderKafkaPublisher kafkaPublisher;

    private String inventoryUrl() {
        String url = System.getenv("INVENTORY_URL");
        return (url != null && !url.isEmpty()) ? url : "http://localhost:8086";
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    public Order createOrder(Order order) {

        if (order.getDetails().isEmpty()) {
            throw new BusinessRuleException("2001", HttpStatus.BAD_REQUEST, "Pedido sin productos");
        }

        for (OrderDetail detail : order.getDetails()) {

            if (order.getDetails() == null || order.getDetails().isEmpty()) {
                throw new BusinessRuleException("2001", HttpStatus.BAD_REQUEST, "Pedido sin productos");
            }

            try {
                // Consultar producto en Inventory
                InventoryResponse product = webClientBuilder.build()
                        .get()
                        .uri(inventoryUrl() + "/inventory/v1/" + detail.getProductId())
                        .retrieve()
                        .bodyToMono(InventoryResponse.class)
                        .block();

                if (product == null) {
                    throw new BusinessRuleException("2002", HttpStatus.NOT_FOUND, "Producto no existe");
                }

                // Validar stock
                if (product.getStock() < detail.getQuantity()) {
                    throw new BusinessRuleException("2003", HttpStatus.BAD_REQUEST, "Stock insuficiente");
                }

                // DESCONTAR STOCK
                webClientBuilder.build()
                        .put()
                        .uri(inventoryUrl() + "/inventory/v1/" + detail.getProductId() + "/stock?quantity=" + (-detail.getQuantity()))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();

            } catch (Exception ex) {
                ex.printStackTrace(); // 👈 para ver errores reales
                throw new BusinessRuleException(
                        "5020",
                        HttpStatus.BAD_GATEWAY,
                        "Error comunicando con Inventory"
                );
            }

            // relación
            detail.setOrder(order);
        }

        order.setOrderNumber(generateOrderNumber());
        order.setStatus("CREADO");

        Order savedOrder = orderRepository.save(order);
        kafkaPublisher.publish(OrderKafkaPublisher.ORDER_CREATED, savedOrder);
        return savedOrder;
    }
    public List<Order> getAll() {
        return orderRepository.findAll();
    }
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException(
                        "2005",
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado"
                ));
    }
    public Order changeStatus(Long id, String newStatus) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException(
                        "2004",
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado"
                ));

        switch (newStatus.toUpperCase()) {

            case "APROBADO":

                if (!order.getStatus().equals("CREADO")) {
                    throw new BusinessRuleException(
                            "2005",
                            HttpStatus.BAD_REQUEST,
                            "Solo pedidos en estado CREADO pueden aprobarse"
                    );
                }

                order.setStatus("APROBADO");
                break;

            case "RECHAZADO":

                if (!order.getStatus().equals("CREADO")) {
                    throw new BusinessRuleException(
                            "2006",
                            HttpStatus.BAD_REQUEST,
                            "Solo pedidos en estado CREADO pueden rechazarse"
                    );
                }

                order.setStatus("RECHAZADO");
                break;

            default:
                throw new BusinessRuleException(
                        "2007",
                        HttpStatus.BAD_REQUEST,
                        "Estado inválido"
                );
        }

        Order savedOrder = orderRepository.save(order);

        if ("APROBADO".equals(savedOrder.getStatus())) {
            // shipment crea el envío al consumir este evento: si no se puede publicar, el pedido no
            // puede quedar aprobado sin envío, así que vuelve a CREADO para reintentar después
            try {
                kafkaPublisher.publishAndWait(OrderKafkaPublisher.ORDER_APPROVED, savedOrder);
            } catch (Exception e) {
                savedOrder.setStatus("CREADO");
                orderRepository.save(savedOrder);
                throw new BusinessRuleException(
                        "2009",
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo despachar el pedido porque Kafka no está disponible, intenta nuevamente"
                );
            }
        } else if ("RECHAZADO".equals(savedOrder.getStatus())) {
            kafkaPublisher.publish(OrderKafkaPublisher.ORDER_REJECTED, savedOrder);
        }

        return savedOrder;
    }

    // Lo invoca el consumidor Kafka de shipment-events
    public Order registerShipmentStatus(Long orderId, String shipmentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException(
                        "2008",
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado para el evento de envío: " + orderId
                ));

        order.setShipmentStatus(shipmentStatus);
        return orderRepository.save(order);
    }
}