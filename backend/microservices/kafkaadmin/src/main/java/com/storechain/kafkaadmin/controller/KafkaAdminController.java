package com.storechain.kafkaadmin.controller;

import com.storechain.kafkaadmin.dto.PartitionsRequest;
import com.storechain.kafkaadmin.dto.TopicConfigRequest;
import com.storechain.kafkaadmin.dto.TopicRequest;
import com.storechain.kafkaadmin.service.KafkaAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kafka-admin/v1")
public class KafkaAdminController {

    @Autowired
    private KafkaAdminService service;

    @GetMapping("/cluster")
    public ResponseEntity<Map<String, Object>> cluster() {
        return ResponseEntity.ok(service.cluster());
    }

    // ---- tópicos ----

    @GetMapping("/topics")
    public ResponseEntity<List<Map<String, Object>>> listTopics() {
        return ResponseEntity.ok(service.listTopics());
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<Map<String, Object>> describeTopic(@PathVariable("name") String name) {
        return ResponseEntity.ok(service.describeTopic(name));
    }

    @PostMapping("/topics")
    public ResponseEntity<Map<String, Object>> createTopic(@RequestBody TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTopic(request));
    }

    @PutMapping("/topics/{name}/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@PathVariable("name") String name,
                                                            @RequestBody TopicConfigRequest request) {
        return ResponseEntity.ok(service.updateTopicConfig(name, request));
    }

    @PutMapping("/topics/{name}/partitions")
    public ResponseEntity<Map<String, Object>> increasePartitions(@PathVariable("name") String name,
                                                                  @RequestBody PartitionsRequest request) {
        return ResponseEntity.ok(service.increasePartitions(name, request));
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> deleteTopic(@PathVariable("name") String name) {
        service.deleteTopic(name);
        return ResponseEntity.noContent().build();
    }

    // ---- consumer groups ----

    @GetMapping("/consumer-groups")
    public ResponseEntity<List<Map<String, Object>>> listGroups() {
        return ResponseEntity.ok(service.listConsumerGroups());
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Map<String, Object>> describeGroup(@PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(service.describeConsumerGroup(groupId));
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable("groupId") String groupId) {
        service.deleteConsumerGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
