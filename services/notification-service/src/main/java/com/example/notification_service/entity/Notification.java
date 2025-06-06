package com.example.notification_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.commons.enums.NotificationStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private Long ticketId;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;

    private int attemptCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(length = 64)
    private String notificationType; // Np. "TICKET_CONFIRMATION_EMAIL"
}