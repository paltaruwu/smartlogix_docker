package com.storechain.kafkaadmin.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TopicRequest {
    private String name;
    private Integer partitions;
    private Short replicationFactor;
    // Configuraciones del tópico, por ejemplo {"retention.ms": "604800000", "cleanup.policy": "delete"}
    private Map<String, String> configs;
}
