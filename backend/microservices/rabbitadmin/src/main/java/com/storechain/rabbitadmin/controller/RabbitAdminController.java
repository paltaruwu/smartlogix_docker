package com.storechain.rabbitadmin.controller;

import com.storechain.rabbitadmin.dto.BindingRequest;
import com.storechain.rabbitadmin.dto.ExchangeRequest;
import com.storechain.rabbitadmin.dto.PublishRequest;
import com.storechain.rabbitadmin.dto.QueueRequest;
import com.storechain.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rabbit-admin/v1")
public class RabbitAdminController {

    @Autowired
    private RabbitAdminService rabbitAdminService;

    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> createQueue(@Valid @RequestBody QueueRequest request) {
        Map<String, Object> result = rabbitAdminService.createQueue(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<Map<String, Object>> getQueue(@PathVariable("name") String name) {
        Map<String, Object> info = rabbitAdminService.getQueueInfo(name);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable("name") String name) {
        rabbitAdminService.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> createExchange(@Valid @RequestBody ExchangeRequest request) {
        Map<String, Object> result = rabbitAdminService.createExchange(request);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable("name") String name) {
        rabbitAdminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.deleteBinding(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> createBinding(@Valid @RequestBody BindingRequest request) {
        Map<String, Object> result = rabbitAdminService.createBinding(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/publish")
    public ResponseEntity<Void> publish(@Valid @RequestBody PublishRequest request) {
        rabbitAdminService.publishMessage(request.getExchange(), request.getRoutingKey(), request.getPayload());
        return ResponseEntity.ok().build();
    }
}
