package com.storechain.notification;

import com.storechain.notification.entities.Notification;
import com.storechain.notification.repository.NotificationRepository;
import com.storechain.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationApplicationTests {

    private NotificationService service() {
        NotificationRepository repository = Mockito.mock(NotificationRepository.class);
        Mockito.when(repository.save(Mockito.any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        NotificationService service = new NotificationService();
        ReflectionTestUtils.setField(service, "repository", repository);
        return service;
    }

    @Test
    void buildsTechnicianNotification() {
        Notification n = service().handleEvent("technician.service.confirmed",
                Map.of("client", "Ana", "message", "Técnico asignado"));
        assertEquals("Técnico asignado", n.getTitle());
        assertEquals("Ana", n.getRecipient());
    }

    @Test
    void buildsRejectedPaymentNotification() {
        Notification n = service().handleEvent("payment.result.technician",
                Map.of("client", "Ana", "status", "RECHAZADO", "amount", 25000, "reason", "Tarjeta rechazada"));
        assertEquals("Pago rechazado", n.getTitle());
    }
}
