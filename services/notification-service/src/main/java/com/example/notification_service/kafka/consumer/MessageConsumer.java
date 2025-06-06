package com.example.notification_service.kafka.consumer;

import com.example.notification_service.service.NotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.example.commons.dto.*;
import lombok.RequiredArgsConstructor;
import org.example.commons.events.ReservationCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);
    private final NotificationService notificationService;
    private final Validator validator;

    @KafkaListener(topics = "cinema.notification", groupId = "cinema-group")
    public void listen(TicketDTO ticketDTO) {
        LOG.info("Received ticket DTO: {}", ticketDTO);

        if (ticketDTO == null) {
            LOG.error("Received null TicketDTO. Message will be skipped");
            return;
        }

        Set<ConstraintViolation<TicketDTO>> violations = validator.validate(ticketDTO);
        if (!violations.isEmpty()) {
            violations.forEach(violation -> LOG.error("Validation error for TicketDTO (ID: {}): {} - {}",
                    ticketDTO.getId(), violation.getPropertyPath(), violation.getMessage()));
            LOG.error("Invalid TicketDTO received (ID: {}). Message will be skipped or sent to DLT. Violations: {}", ticketDTO.getId(), violations);
            return;
        }

        try {
            notificationService.processAndSendTicketNotification(ticketDTO);
        } catch (IllegalArgumentException e) {
            LOG.error("Illegal argument while processing notification for ticket ID {}: {}. Message might be problematic.",
                    ticketDTO.getId(), e.getMessage(), e);
        }
        catch (Exception e) {
            LOG.error("Unexpected error processing notification for ticket ID {}: {}",
                    ticketDTO.getId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "cinema.cancel.reservation",
            groupId = "cinema-group",
            containerFactory = "reservationCancelledEventKafkaListenerContainerFactory")
    public void listenToReservationCancelled(ReservationCancelledEvent event) {
        LOG.info("Received ReservationCancelledEvent for reservation ID: {}", event.getReservationId());

        if (event.getReservationId() == null) {
            LOG.error("Received invalid ReservationCancelledEvent (null or missing reservationId). Skipping.");
            return;
        }

        if (Objects.equals(event.getCancellationReason(), "USER")) {
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isEmpty()) {
                LOG.error("Customer email is missing in ReservationCancelledEvent for reservation ID {}. Cannot send user notification.", event.getReservationId());
                return;
            }
            try {
                notificationService.processAndSendReservationCancelledNotification(event);
            } catch (Exception e) {
                LOG.error("Unexpected error processing user notification for cancelled reservation ID {}: {}", event.getReservationId(), e.getMessage(), e);
            }
        } else {
            LOG.info("ReservationCancelledEvent for reservation ID {} received, but sendUserNotification is false. Skipping email notification to user.", event.getReservationId());
        }
    }

    @KafkaListener(topics = "cinema.notification.screening_change", groupId = "cinema-group-notification",
            containerFactory = "screeningChangeNotificationKafkaListenerContainerFactory") // Dedykowana fabryka
    public void listenToScreeningChangeNotification(ScreeningChangeNotificationDTO payload) {
        LOG.info("Received screening change notification: {}", payload);
        if (payload == null || payload.getCustomerEmail() == null || payload.getCustomerEmail().isEmpty()) {
            LOG.error("Received invalid ScreeningChangeNotificationDTO (null or no email). Skipping. Payload: {}", payload);
            // Można rzucić wyjątek do DLT
            return;
        }
        // Walidacja DTO, jeśli potrzebna, chociaż podstawowa powinna być w payloadzie
        // Set<ConstraintViolation<ScreeningChangeNotificationDTO>> violations = validator.validate(payload);
        // if (!violations.isEmpty()) { ... }
        try {
            notificationService.processAndSendScreeningChangeNotification(payload);
        } catch (Exception e) {
            LOG.error("Unexpected error processing screening change notification for reservation ID {}: {}",
                    payload.getReservationId(), e.getMessage(), e);
            // Rozważ rzucenie do DLT
        }
    }
}
