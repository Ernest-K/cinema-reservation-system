package com.example.notification_service.repository;

import com.example.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByTicketIdAndNotificationType(Long ticketId, String notificationType);

    Optional<Notification> findByReservationIdAndNotificationType(Long reservationId, String notificationType);

    Optional<Notification> findTopByTicketIdAndNotificationTypeOrderByCreatedAtDesc(Long ticketId, String notificationType);
}
