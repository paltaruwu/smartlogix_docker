package com.storechain.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de stock publicado en el tópico inventory-events.
 * eventType: STOCK_REPLENISHED (se repuso stock desde el panel Admin).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockEvent {
    private String eventId;
    private String eventType;
    private Long productId;
    private String productTitle;
    private Integer quantity;
    private Double stockAfter;
    private String reason;
    private String occurredAt;
}
