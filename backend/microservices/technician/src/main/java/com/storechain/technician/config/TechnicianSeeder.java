package com.storechain.technician.config;

import com.storechain.technician.entities.Technician;
import com.storechain.technician.repository.TechnicianRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TechnicianSeeder {

    // Datos de ejemplo la primera vez que arranca (tabla vacía)
    @Bean
    CommandLineRunner seedTechnicians(TechnicianRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(List.of(
                    tech("Juan Pérez", "Reparación de laptops",
                            "Cambio de pantalla, teclado, batería y limpieza interna.", 25000.0),
                    tech("María González", "Redes y WiFi",
                            "Instalación de routers, cableado y optimización de señal.", 30000.0),
                    tech("Carlos Rojas", "Soporte de PC de escritorio",
                            "Armado, formateo, antivirus y mejora de rendimiento.", 20000.0),
                    tech("Ana Soto", "Recuperación de datos",
                            "Rescate de archivos de discos dañados y respaldos.", 45000.0)
            ));
        };
    }

    private Technician tech(String name, String specialty, String description, double price) {
        Technician t = new Technician();
        t.setName(name);
        t.setSpecialty(specialty);
        t.setDescription(description);
        t.setPrice(price);
        t.setAvailable(true);
        return t;
    }
}
