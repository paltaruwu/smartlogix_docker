package com.storechain.technician.repository;

import com.storechain.technician.entities.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    // Reserva atómica: solo uno de dos clientes simultáneos obtiene al técnico
    @Transactional
    @Modifying
    @Query("UPDATE Technician t SET t.available = false WHERE t.id = :id AND t.available = true")
    int reserve(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("UPDATE Technician t SET t.available = true WHERE t.id = :id")
    int release(@Param("id") Long id);
}
