package com.storechain.notification.repository;

import com.storechain.notification.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByIdDesc();

    List<Notification> findByRecipientIgnoreCaseOrderByIdDesc(String recipient);
}
