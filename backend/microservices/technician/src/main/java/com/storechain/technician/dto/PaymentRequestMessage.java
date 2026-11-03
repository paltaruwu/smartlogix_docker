package com.storechain.technician.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestMessage {
    private String referenceType;
    private Long referenceId;
    private String client;
    private Double amount;
    private String cardLast4;
    private String description;
}
