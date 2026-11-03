package com.storechain.notification.service;

import com.storechain.notification.entities.Notification;
import com.storechain.notification.exception.BusinessRuleException;
import com.storechain.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    /** Convierte un evento de RabbitMQ en una notificación para el cliente y la guarda. */
    public Notification handleEvent(String routingKey, Map<String, Object> payload) {
        Notification n = new Notification();
        n.setEventType(routingKey);
        n.setRecipient(text(payload.get("client"), "Cliente"));
        n.setEmail(text(payload.get("email"), null));
        n.setChannel("EMAIL (simulado)");
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now().toString());

        if (routingKey.startsWith("technician.service.")) {
            n.setTitle(technicianTitle(routingKey));
            n.setMessage(text(payload.get("message"), "Actualización de tu servicio técnico"));
        } else if (routingKey.startsWith("payment.result.")) {
            boolean ok = "APROBADO".equals(payload.get("status"));
            n.setTitle(ok ? "Pago aprobado" : "Pago rechazado");
            n.setMessage("Pago de $" + amount(payload.get("amount")) + ": " + text(payload.get("reason"), ""));
        } else if (routingKey.startsWith("logistics.shipment.dispatch")) {
            n.setTitle("Pedido en camino");
            n.setMessage("Tu pedido " + text(payload.get("orderNumber"), "") + " fue aprobado y será despachado a "
                    + text(payload.get("address"), "tu dirección") + ".");
        } else {
            n.setTitle("Notificación");
            n.setMessage("Evento recibido: " + routingKey);
        }

        Notification saved = repository.save(n);
        log.info("[EMAIL SIMULADO] Para: {} | Asunto: {} | {}", n.getRecipient(), n.getTitle(), n.getMessage());
        return saved;
    }

    public List<Notification> list(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return repository.findAllByOrderByIdDesc();
        }
        return repository.findByRecipientIgnoreCaseOrderByIdDesc(recipient);
    }

    public Notification markAsRead(Long id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("6001", HttpStatus.NOT_FOUND, "Notificación no encontrada"));
        n.setReadFlag(true);
        return repository.save(n);
    }

    private String technicianTitle(String routingKey) {
        return switch (routingKey.substring("technician.service.".length())) {
            case "requested" -> "Solicitud recibida";
            case "confirmed" -> "Técnico asignado";
            case "started" -> "Técnico en camino";
            case "finished" -> "Servicio finalizado";
            case "cancelled" -> "Solicitud cancelada";
            default -> "Servicio técnico";
        };
    }

    private String text(Object value, String fallback) {
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    private String amount(Object value) {
        if (value instanceof Number number) {
            return String.format("%,.0f", number.doubleValue()).replace(',', '.');
        }
        return String.valueOf(value);
    }
}
