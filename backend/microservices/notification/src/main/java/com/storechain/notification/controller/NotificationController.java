package com.storechain.notification.controller;

import com.storechain.notification.entities.Notification;
import com.storechain.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification/v1")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @GetMapping
    public ResponseEntity<List<Notification>> list(
            @RequestParam(value = "client", required = false) String client) {
        return ResponseEntity.ok(service.list(client));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.markAsRead(id));
    }
}
