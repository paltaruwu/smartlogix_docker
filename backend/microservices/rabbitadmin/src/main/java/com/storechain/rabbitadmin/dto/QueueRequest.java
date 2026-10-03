package com.storechain.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueueRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String name;

    private boolean durable = true;

    private boolean withDlq = false;
}
