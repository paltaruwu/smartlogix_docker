package com.storechain.technician.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long technicianId;
    private String technicianName;
    private String specialty;

    private String client;
    private String email;
    private String address;

    @Column(length = 500)
    private String description;

    private Double amount;

    private String status; // SOLICITADO, PAGADO, EN_CURSO, FINALIZADO, CANCELADO
    private String paymentStatus; // PENDIENTE, APROBADO, RECHAZADO
    private String paymentReason;
    private String createdAt;
}
