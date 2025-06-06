package com.example.ticket_service.kafka.consumer;


import com.example.ticket_service.entity.Ticket;
import com.example.ticket_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.ScreeningDTO;
import org.example.commons.enums.TicketStatus;
import org.example.commons.events.ScreeningCancelledEvent;
import org.example.commons.events.ScreeningUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScreeningEventsConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(ScreeningEventsConsumer.class);
    private final TicketRepository ticketRepository;

    public static final String SCREENING_UPDATED_TOPIC_TICKET = "cinema.screenings.updated.for-ticket"; // Inny topic lub ta sama grupa co reservation-service
    public static final String SCREENING_CANCELLED_TOPIC_TICKET = "cinema.screenings.cancelled.for-ticket";

    @KafkaListener(topics = "cinema.screenings.updated", // Użyj odpowiednich nazw topiców i fabryk
            groupId = "cinema-group-ticket", // lub dedykowana
            containerFactory = "screeningUpdatedEventKafkaListenerContainerFactory") // Dedykowana
    @Transactional
    public void handleScreeningUpdated(ScreeningUpdatedEvent event) {
        LOG.info("Received ScreeningUpdatedEvent for screening ID: {} in TicketService", event.getScreeningId());
        List<Ticket> ticketsForScreening = ticketRepository.findAllByScreeningId(event.getScreeningId());
        if (ticketsForScreening.isEmpty()) return;

        ScreeningDTO updatedDetails = event.getUpdatedScreeningDTO();
        for (Ticket ticket : ticketsForScreening) {
            // Aktualizuj tylko jeśli bilet jest wciąż ważny
            if (ticket.getStatus() == TicketStatus.VALID || ticket.getStatus() == TicketStatus.USED) {
                ticket.setScreeningStartTime(updatedDetails.getStartTime());
                ticket.setMovieTitle(updatedDetails.getMovieDTO().getTitle()); // Jeśli tytuł może się zmienić
                ticket.setHallNumber(updatedDetails.getHallDTO().getNumber()); // Jeśli sala może się zmienić
                // TODO: Przebudować QR code data, jeśli zawiera te informacje.
                // To może być skomplikowane. Prostsze może być wysłanie powiadomienia o zmianie.
                // ticket.setQrCodeData(qrCodeGeneratorService.generateQrCodeText(...nowe dane...));
                ticketRepository.save(ticket);
                LOG.info("Ticket ID {} updated due to screening {} update.", ticket.getId(), event.getScreeningId());
                // TODO: Wysłać powiadomienie do użytkownika o zmianie danych seansu na jego bilecie.
            }
        }
    }

    @KafkaListener(topics = "cinema.screenings.cancelled",
            groupId = "cinema-group-ticket",
            containerFactory = "screeningCancelledEventKafkaListenerContainerFactory")
    @Transactional
    public void handleScreeningCancelled(ScreeningCancelledEvent event) {
        LOG.info("Received ScreeningCancelledEvent for screening ID: {} in TicketService", event.getScreeningId());
        List<Ticket> ticketsForScreening = ticketRepository.findAllByScreeningId(event.getScreeningId());
        if (ticketsForScreening.isEmpty()) return;

        for (Ticket ticket : ticketsForScreening) {
            if (ticket.getStatus() != TicketStatus.CANCELLED) {
                if (ticket.getStatus() == TicketStatus.USED) {
                    LOG.warn("Screening ID {} was cancelled, but ticket ID {} was already USED. Marking as CANCELLED_AFTER_USE (conceptually).",
                            event.getScreeningId(), ticket.getId());
                    // Możesz dodać logikę specjalnego oznaczenia lub tylko log.
                    // Obecnie, jeśli USED, nie zmieniamy na CANCELLED.
                } else {
                    ticket.setStatus(TicketStatus.CANCELLED);
                    ticketRepository.save(ticket);
                    LOG.info("Ticket ID {} marked as CANCELLED due to screening {} cancellation.", ticket.getId(), event.getScreeningId());
                    // TODO: Wysłać powiadomienie do użytkownika o anulowaniu jego biletu.
                }
            }
        }
    }
}