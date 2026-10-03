package com.storechain.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublishRequest {

    @NotBlank(message = "El exchange es obligatorio")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    private String routingKey;

    private Object payload;
}
