package com.storechain.payment.service;

import com.storechain.payment.dto.PaymentRequestMessage;
import com.storechain.payment.dto.PaymentResultMessage;
import com.storechain.payment.entities.Payment;
import com.storechain.payment.exception.BusinessRuleException;
import com.storechain.payment.repository.PaymentRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.topic}")
    private String topicExchange;

    @Value("${rabbitmq.routing.key.payment.result.prefix}")
    private String resultPrefix;

    @Value("${payment.max-amount}")
    private double maxAmount;

    @Value("${payment.reject-card-last4}")
    private String rejectCardLast4;

    /**
     * Procesa una solicitud de pago y publica el resultado. Es idempotente: si ya existe un pago
     * para la misma referencia (mensaje repetido), reutiliza el resultado anterior.
     */
    public Payment process(PaymentRequestMessage request) {
        if (request.getReferenceType() == null || request.getReferenceId() == null) {
            throw new BusinessRuleException("5001", HttpStatus.BAD_REQUEST, "Referencia del pago es obligatoria");
        }

        Payment payment = repository
                .findByReferenceTypeAndReferenceId(request.getReferenceType(), request.getReferenceId())
                .orElseGet(() -> repository.save(evaluate(request)));

        publishResult(payment);
        return payment;
    }

    private Payment evaluate(PaymentRequestMessage request) {
        Payment payment = new Payment();
        payment.setReferenceType(request.getReferenceType());
        payment.setReferenceId(request.getReferenceId());
        payment.setClient(request.getClient());
        payment.setAmount(request.getAmount());
        payment.setCardLast4(request.getCardLast4());
        payment.setDescription(request.getDescription());
        payment.setCreatedAt(LocalDateTime.now().toString());

        String reason = rejectionReason(request);
        payment.setStatus(reason == null ? "APROBADO" : "RECHAZADO");
        payment.setReason(reason == null ? "Pago aprobado" : reason);
        return payment;
    }

    private String rejectionReason(PaymentRequestMessage request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            return "Monto inválido";
        }
        if (request.getAmount() > maxAmount) {
            return "Monto excede el límite permitido";
        }
        if (rejectCardLast4.equals(request.getCardLast4())) {
            return "Tarjeta rechazada por el banco";
        }
        return null;
    }

    private void publishResult(Payment payment) {
        PaymentResultMessage result = new PaymentResultMessage(
                payment.getId(), payment.getReferenceType(), payment.getReferenceId(),
                payment.getClient(), payment.getAmount(), payment.getStatus(), payment.getReason());
        String key = resultPrefix + "." + payment.getReferenceType().toLowerCase();
        rabbitTemplate.convertAndSend(topicExchange, key, result);
    }

    public List<Payment> list() {
        return repository.findAll();
    }

    public Payment get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("5002", HttpStatus.NOT_FOUND, "Pago no encontrado"));
    }
}
