package com.storechain.inventory.repository;

import com.storechain.inventory.entities.LowStockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {

    List<LowStockAlert> findAllByOrderByIdDesc();

    List<LowStockAlert> findByProductId(Long productId);
}
