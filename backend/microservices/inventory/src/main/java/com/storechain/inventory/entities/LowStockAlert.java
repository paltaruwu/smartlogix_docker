package com.storechain.inventory.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** Alerta generada cuando, tras un pedido, el stock de un producto queda bajo el umbral. */
@Data
@Entity
@Table(name = "low_stock_alerts")
public class LowStockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productTitle;
    private Double stock;
    private Integer threshold;
    private String orderNumber;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
