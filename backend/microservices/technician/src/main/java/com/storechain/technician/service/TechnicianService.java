package com.storechain.technician.service;

import com.storechain.technician.dto.*;
import com.storechain.technician.entities.ServiceRequest;
import com.storechain.technician.entities.Technician;
import com.storechain.technician.exception.BusinessRuleException;
import com.storechain.technician.messaging.TechnicianEventPublisher;
import com.storechain.technician.repository.ServiceRequestRepository;
import com.storechain.technician.repository.TechnicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class TechnicianService {

    private static final Set<String> MANUAL_STATUSES = Set.of("EN_CURSO", "FINALIZADO", "CANCELADO");

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private ServiceRequestRepository requestRepository;

    @Autowired
    private TechnicianEventPublisher publisher;

    public List<Technician> catalog() {
        return technicianRepository.findAll();
    }

    public Technician createTechnician(Technician t) {
        if (t.getName() == null || t.getName().isBlank() || t.getPrice() == null || t.getPrice() <= 0) {
            throw new BusinessRuleException("4001", HttpStatus.BAD_REQUEST, "Nombre y precio válido son obligatorios");
        }
        t.setId(null);
        t.setAvailable(true);
        return technicianRepository.save(t);
    }

    // Sin @Transactional a propósito: la solicitud debe estar confirmada en la BD
    // antes de publicar el pago, o la respuesta de Payment podría llegar antes del commit.
    public ServiceRequest hire(HireRequest req) {
        if (req.getTechnicianId() == null || isBlank(req.getClient()) || isBlank(req.getAddress())
                || isBlank(req.getDescription())) {
            throw new BusinessRuleException("4002", HttpStatus.BAD_REQUEST,
                    "Técnico, nombre, dirección y descripción del problema son obligatorios");
        }
        String digits = req.getCardNumber() == null ? "" : req.getCardNumber().replaceAll("\\D", "");
        if (digits.length() < 4) {
            throw new BusinessRuleException("4003", HttpStatus.BAD_REQUEST, "Tarjeta inválida");
        }

        Technician technician = technicianRepository.findById(req.getTechnicianId())
                .orElseThrow(() -> new BusinessRuleException("4004", HttpStatus.NOT_FOUND, "Técnico no existe"));

        // Reserva atómica: si dos clientes contratan a la vez, solo uno la obtiene
        if (technicianRepository.reserve(technician.getId()) == 0) {
            throw new BusinessRuleException("4005", HttpStatus.CONFLICT, "El técnico ya no está disponible");
        }

        ServiceRequest sr = new ServiceRequest();
        sr.setTechnicianId(technician.getId());
        sr.setTechnicianName(technician.getName());
        sr.setSpecialty(technician.getSpecialty());
        sr.setClient(req.getClient());
        sr.setEmail(req.getEmail());
        sr.setAddress(req.getAddress());
        sr.setDescription(req.getDescription());
        sr.setAmount(technician.getPrice());
        sr.setStatus("SOLICITADO");
        sr.setPaymentStatus("PENDIENTE");
        sr.setCreatedAt(LocalDateTime.now().toString());
        sr = requestRepository.save(sr);

        try {
            publisher.publishPaymentRequest(new PaymentRequestMessage(
                    "TECHNICIAN", sr.getId(), sr.getClient(), sr.getAmount(),
                    digits.substring(digits.length() - 4),
                    "Servicio técnico: " + technician.getName()));
        } catch (Exception e) {
            // No se pudo pedir el pago: se cancela la solicitud y el técnico queda libre
            sr.setStatus("CANCELADO");
            sr.setPaymentReason("No se pudo contactar al servicio de pagos");
            requestRepository.save(sr);
            technicianRepository.release(technician.getId());
            throw new BusinessRuleException("4009", HttpStatus.SERVICE_UNAVAILABLE,
                    "Servicio de pagos no disponible, intenta nuevamente");
        }

        publisher.publishServiceEvent(event(sr, "requested",
                "Recibimos tu solicitud con " + sr.getTechnicianName() + ". Estamos procesando el pago."));
        return sr;
    }

    @Transactional
    public void applyPaymentResult(PaymentResultMessage result) {
        ServiceRequest sr = requestRepository.findById(result.getReferenceId())
                .orElseThrow(() -> new BusinessRuleException("4006", HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (!"SOLICITADO".equals(sr.getStatus())) {
            return; // mensaje repetido: ya fue procesado
        }

        sr.setPaymentStatus(result.getStatus());
        sr.setPaymentReason(result.getReason());

        if ("APROBADO".equals(result.getStatus())) {
            sr.setStatus("PAGADO");
            requestRepository.save(sr);
            publisher.publishServiceEvent(event(sr, "confirmed",
                    "Pago aprobado. " + sr.getTechnicianName() + " fue asignado a tu servicio."));
        } else {
            sr.setStatus("CANCELADO");
            requestRepository.save(sr);
            technicianRepository.release(sr.getTechnicianId());
            publisher.publishServiceEvent(event(sr, "cancelled",
                    "Tu pago fue rechazado (" + result.getReason() + "). La solicitud fue cancelada."));
        }
    }

    public List<ServiceRequest> requests() {
        return requestRepository.findAll();
    }

    public ServiceRequest track(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("4006", HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
    }

    @Transactional
    public ServiceRequest changeStatus(Long id, String status) {
        String newStatus = status == null ? "" : status.toUpperCase();
        if (!MANUAL_STATUSES.contains(newStatus)) {
            throw new BusinessRuleException("4007", HttpStatus.BAD_REQUEST, "Estado inválido");
        }
        ServiceRequest sr = track(id);
        String current = sr.getStatus();

        boolean valid = switch (newStatus) {
            case "EN_CURSO" -> "PAGADO".equals(current);
            case "FINALIZADO" -> "EN_CURSO".equals(current) || "PAGADO".equals(current);
            default -> "SOLICITADO".equals(current) || "PAGADO".equals(current); // CANCELADO
        };
        if (!valid) {
            throw new BusinessRuleException("4008", HttpStatus.BAD_REQUEST,
                    "No se puede pasar de " + current + " a " + newStatus);
        }

        sr.setStatus(newStatus);
        sr = requestRepository.save(sr);
        if (!"EN_CURSO".equals(newStatus)) {
            technicianRepository.release(sr.getTechnicianId());
        }

        String text = switch (newStatus) {
            case "EN_CURSO" -> sr.getTechnicianName() + " va en camino a tu dirección.";
            case "FINALIZADO" -> "El servicio con " + sr.getTechnicianName() + " fue finalizado. Gracias por confiar en SmartLogix.";
            default -> "Tu solicitud fue cancelada.";
        };
        String eventName = switch (newStatus) {
            case "EN_CURSO" -> "started";
            case "FINALIZADO" -> "finished";
            default -> "cancelled";
        };
        publisher.publishServiceEvent(event(sr, eventName, text));
        return sr;
    }

    private ServiceEventMessage event(ServiceRequest sr, String event, String message) {
        return new ServiceEventMessage(sr.getId(), event, sr.getClient(), sr.getEmail(), sr.getTechnicianName(), message);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
