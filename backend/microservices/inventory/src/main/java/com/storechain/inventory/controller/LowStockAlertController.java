package com.storechain.inventory.controller;

import com.storechain.inventory.entities.LowStockAlert;
import com.storechain.inventory.service.LowStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory/v1/alerts")
public class LowStockAlertController {

    @Autowired
    private LowStockService service;

    @GetMapping
    public ResponseEntity<List<LowStockAlert>> list() {
        return ResponseEntity.ok(service.listAlerts());
    }
}
