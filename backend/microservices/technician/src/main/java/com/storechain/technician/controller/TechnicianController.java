package com.storechain.technician.controller;

import com.storechain.technician.dto.HireRequest;
import com.storechain.technician.entities.ServiceRequest;
import com.storechain.technician.entities.Technician;
import com.storechain.technician.service.TechnicianService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/technician/v1")
public class TechnicianController {

    @Autowired
    private TechnicianService service;

    @GetMapping
    public ResponseEntity<List<Technician>> catalog() {
        return ResponseEntity.ok(service.catalog());
    }

    @PostMapping
    public ResponseEntity<Technician> create(@RequestBody Technician technician) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTechnician(technician));
    }

    @PostMapping("/hire")
    public ResponseEntity<ServiceRequest> hire(@RequestBody HireRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.hire(request));
    }

    @GetMapping("/track/{id}")
    public ResponseEntity<ServiceRequest> track(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.track(id));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<ServiceRequest>> requests() {
        return ResponseEntity.ok(service.requests());
    }

    @PutMapping("/requests/{id}/status")
    public ResponseEntity<ServiceRequest> changeStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(service.changeStatus(id, status));
    }
}
