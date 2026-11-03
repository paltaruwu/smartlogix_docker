package com.storechain.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Evento del ciclo de vida de un pedido, publicado en el tópico order-events.
 * eventType: ORDER_CREATED, ORDER_APPROVED u ORDER_REJECTED.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent {
    private String eventId;
    private String eventType;
    private Long orderId;
    private String orderNumber;
    private String client;
    private String address;
    private String status;
    private List<OrderEventItem> items;
    private String occurredAt;
}
