package com.storechain.payment.controller;

import com.storechain.payment.entities.Payment;
import com.storechain.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment/v1")
public class PaymentController {

    @Autowired
    private PaymentService service;

    @GetMapping
    public ResponseEntity<List<Payment>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.get(id));
    }
}
