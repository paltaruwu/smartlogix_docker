package com.storechain.kafkaadmin.dto;

import lombok.Data;

@Data
public class PartitionsRequest {
    // Cantidad total de particiones deseada (Kafka solo permite aumentarlas)
    private Integer partitions;
}
