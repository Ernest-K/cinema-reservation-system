package com.example.notification_service.kafka.consumer;

import com.example.notification_service.service.NotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.example.commons.dto.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
}
