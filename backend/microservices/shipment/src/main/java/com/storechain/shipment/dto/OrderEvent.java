package com.storechain.shipment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento del ciclo de vida de un pedido, consumido desde el tópico order-events (lo publica order).
 * Es una copia local del contrato: los microservicios no comparten clases, solo el formato JSON.
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
    private String occurredAt;
}
