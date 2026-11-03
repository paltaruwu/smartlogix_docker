package com.storechain.technician.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEventMessage {
    private Long requestId;
    private String event; // requested, confirmed, cancelled, started, finished
    private String client;
    private String email;
    private String technicianName;
    private String message;
}
