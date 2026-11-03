package com.storechain.technician.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String specialty;

    @Column(length = 500)
    private String description;

    private Double price;
    private Boolean available;
    private String imageSrc;
}
