package com.storechain.payment.repository;

import com.storechain.payment.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
