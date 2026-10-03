package com.storechain.shipment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDispatchMessage {
    private Long orderId;
    private String orderNumber;
    private String client;
    private String address;
}
