package com.storechain.notification.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType; // routing key que originó la notificación
    private String recipient;
    private String email;
    private String title;

    @Column(length = 500)
    private String message;

    private String channel; // EMAIL (simulado)
    private Boolean readFlag;
    private String createdAt;
}
