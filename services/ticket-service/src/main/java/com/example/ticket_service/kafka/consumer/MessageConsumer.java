package com.example.ticket_service.kafka.consumer;

import org.example.commons.dto.*;
import com.example.ticket_service.entity.Ticket;
import com.example.ticket_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            Ticket ticket = ticketService.generateAndSaveTicketForReservation(reservationDTO);

            LOG.info("Successfully processed ticket generation for reservation ID: {}. Ticket UID: {}",
                    reservationDTO.getId(), ticket.getId());

        } catch (IllegalArgumentException e) {
            LOG.error("Invalid data for ticket generation for reservation {}: {}", reservationDTO.getId(), e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // Ten błąd może wystąpić, jeśli jakimś cudem wiadomość zostanie przetworzona dwa razy
            // a idempotentność w serwisie nie przechwyci tego przed próbą zapisu do bazy.
            // Dzięki constraintowi UNIQUE na reservation_id, druga próba zapisu się nie powiedzie.
            LOG.warn("Data integrity violation for reservation ID: {}. Potentially a duplicate message or ticket already exists. Error: {}",
                    reservationDTO.getId(), e.getMessage());
        }
        catch (Exception e) {
            LOG.error("Error processing ticket generation request for reservation: {}", reservationDTO.getId(), e);
        }
    }
}
