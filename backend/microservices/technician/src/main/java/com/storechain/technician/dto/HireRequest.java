package com.storechain.technician.dto;

import lombok.Data;

@Data
public class HireRequest {
    private Long technicianId;
    private String client;
    private String email;
    private String address;
    private String description;
    private String cardNumber; // solo se usan los últimos 4 dígitos; nunca se guarda completo
}
