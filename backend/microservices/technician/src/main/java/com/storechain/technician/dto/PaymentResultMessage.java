package com.storechain.technician.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultMessage {
    private Long paymentId;
    private String referenceType;
    private Long referenceId;
    private String client;
    private Double amount;
    private String status; // APROBADO, RECHAZADO
    private String reason;
}
