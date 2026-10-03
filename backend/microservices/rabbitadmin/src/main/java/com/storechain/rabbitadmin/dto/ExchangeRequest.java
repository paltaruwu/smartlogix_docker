package com.storechain.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ExchangeRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String name;

    @NotBlank(message = "El tipo del exchange es obligatorio")
    @Pattern(regexp = "direct|topic|fanout|headers",
             message = "Tipo debe ser: direct, topic, fanout o headers")
    private String type;

    private boolean durable = true;
}
