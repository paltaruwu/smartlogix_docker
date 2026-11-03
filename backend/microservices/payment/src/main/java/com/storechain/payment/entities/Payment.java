package com.storechain.payment.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"referenceType", "referenceId"}))
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String referenceType; // TECHNICIAN, ORDER...
    private Long referenceId;

    private String client;
    private Double amount;
    private String cardLast4;
    private String description;

    private String status; // APROBADO, RECHAZADO
    private String reason;
    private String createdAt;
}
