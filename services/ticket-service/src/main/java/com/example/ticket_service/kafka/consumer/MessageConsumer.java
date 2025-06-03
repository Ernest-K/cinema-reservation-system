package com.example.ticket_service.kafka.consumer;

import org.example.commons.dto.*;
import com.example.ticket_service.entity.Ticket;
import com.example.ticket_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.example.commons.events.ReservationCancelledEvent;
import org.example.commons.exception.TicketAlreadyExistsException;
import org.example.commons.exception.TicketGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);
    private final TicketService ticketService;

    @KafkaListener(topics = "cinema.ticket.request", groupId = "cinema-group")
    public void listen(ReservationDTO reservationDTO) {
        LOG.info("Received ticket generation request for reservation: {}", reservationDTO);

        if (reservationDTO == null || reservationDTO.getId() == null) {
            LOG.error("Received null or invalid ReservationDTO. Skipping processing");
            return;
        }

        try {
            Ticket ticket = ticketService.generateAndSaveTicketForReservation(reservationDTO);
            LOG.info("Successfully processed ticket generation for reservation ID: {}. Ticket ID: {}",
                    reservationDTO.getId(), ticket.getId());

        } catch (TicketAlreadyExistsException e) {
            LOG.warn("Ticket generation skipped for reservation ID {}: {}. Message likely already processed.",
                    reservationDTO.getId(), e.getMessage());
        } catch (TicketGenerationException e) {
            LOG.error("TicketGenerationException for reservation ID {}: {}. Check if compensations are needed.",
                    reservationDTO.getId(), e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid data for ticket generation for reservation {}: {}", reservationDTO.getId(), e.getMessage(), e);
        }
        catch (Exception e) {
            LOG.error("Unexpected error processing ticket generation request for reservation {}: {}",
                    reservationDTO.getId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "cinema.cancel.reservation", groupId = "cinema-group", containerFactory = "reservationCancelledEventKafkaListenerContainerFactory")
    public void listenToReservationCancellation(ReservationCancelledEvent event) {
        LOG.info("Received reservation cancellation event for reservation ID: {}", event.getReservationId());
        try {
            ticketService.handleReservationCancellation(event.getReservationId());
        } catch (Exception e) {
            LOG.error("Error handling reservation cancellation for ticket linked to reservation ID {}: {}",
                    event.getReservationId(), e.getMessage(), e);
        }
    }
}
