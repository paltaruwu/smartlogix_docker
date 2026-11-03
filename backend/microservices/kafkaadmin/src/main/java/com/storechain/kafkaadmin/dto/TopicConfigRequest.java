package com.storechain.kafkaadmin.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TopicConfigRequest {
    private Map<String, String> configs;
}
