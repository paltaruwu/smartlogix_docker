package com.storechain.payment;

import com.storechain.payment.dto.PaymentRequestMessage;
import com.storechain.payment.entities.Payment;
import com.storechain.payment.repository.PaymentRepository;
import com.storechain.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentApplicationTests {

    private PaymentService service() {
        PaymentRepository repository = Mockito.mock(PaymentRepository.class);
        Mockito.when(repository.findByReferenceTypeAndReferenceId(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.empty());
        Mockito.when(repository.save(Mockito.any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentService service = new PaymentService();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "rabbitTemplate", Mockito.mock(RabbitTemplate.class));
        ReflectionTestUtils.setField(service, "topicExchange", "smartlogix.topic");
        ReflectionTestUtils.setField(service, "resultPrefix", "payment.result");
        ReflectionTestUtils.setField(service, "maxAmount", 2000000.0);
        ReflectionTestUtils.setField(service, "rejectCardLast4", "0000");
        return service;
    }

    private PaymentRequestMessage request(double amount, String last4) {
        return new PaymentRequestMessage("TECHNICIAN", 1L, "Cliente", amount, last4, "test");
    }

    @Test
    void approvesNormalPayment() {
        assertEquals("APROBADO", service().process(request(25000, "4242")).getStatus());
    }

    @Test
    void rejectsDeclinedCard() {
        assertEquals("RECHAZADO", service().process(request(25000, "0000")).getStatus());
    }

    @Test
    void rejectsAmountOverLimit() {
        assertEquals("RECHAZADO", service().process(request(5000000, "4242")).getStatus());
    }
}
