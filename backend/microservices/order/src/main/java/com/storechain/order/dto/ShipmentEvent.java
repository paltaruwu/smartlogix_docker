package com.storechain.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de envío consumido desde el tópico shipment-events (lo publica el microservicio shipment).
 * Es una copia local del contrato: los microservicios no comparten clases, solo el formato JSON.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShipmentEvent {
    private String eventId;
    private String eventType;
    private Long shipmentId;
    private Long orderId;
    private String status;
    private String carrier;
    private String estimatedDate;
    private String occurredAt;
}
