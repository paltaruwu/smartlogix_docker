package com.storechain.shipment.service;

import com.storechain.shipment.entities.Shipment;
import com.storechain.shipment.exception.BusinessRuleException;
import com.storechain.shipment.messaging.ShipmentKafkaPublisher;
import com.storechain.shipment.repository.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ShipmentService {

    @Autowired
    private ShipmentRepository repository;

    @Autowired
    private ShipmentKafkaPublisher kafkaPublisher;

    public Shipment create(Shipment shipment) {
        if (shipment.getOrderId() == null) {
            throw new BusinessRuleException("3001", HttpStatus.BAD_REQUEST, "OrderId es obligatorio");
        }

        shipment.setStatus("ENVIADO");
        shipment.setCarrier("Chile Express");
        shipment.setEstimatedDate(String.valueOf(LocalDate.now().plusDays(3)));

        Shipment saved = repository.save(shipment);
        kafkaPublisher.publish(ShipmentKafkaPublisher.SHIPMENT_CREATED, saved);
        return saved;
    }

    /**
     * Crea el envío de un pedido aprobado. Kafka entrega al menos una vez, así que es idempotente:
     * si el pedido ya tiene envío, no crea otro.
     */
    public Shipment createForOrder(Long orderId, String client, String address) {
        if (repository.existsByOrderId(orderId)) {
            return null;
        }

        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setClient(client);
        shipment.setAddress(address);
        return create(shipment);
    }

    public Shipment updateStatus(Long id, String status) {

        Shipment shipment = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException(
                        "3002",
                        HttpStatus.NOT_FOUND,
                        "Envío no encontrado"
                ));

        shipment.setStatus(status);

        Shipment saved = repository.save(shipment);
        kafkaPublisher.publish(ShipmentKafkaPublisher.SHIPMENT_STATUS_CHANGED, saved);
        return saved;
    }
}
