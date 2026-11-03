package com.storechain.shipment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de envío publicado en el tópico shipment-events.
 * eventType: SHIPMENT_CREATED o SHIPMENT_STATUS_CHANGED.
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
