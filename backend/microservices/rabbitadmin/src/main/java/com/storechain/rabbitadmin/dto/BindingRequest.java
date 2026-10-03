package com.storechain.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindingRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String queueName;

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String exchangeName;

    @NotBlank(message = "La routing key es obligatoria")
    private String routingKey;
}
