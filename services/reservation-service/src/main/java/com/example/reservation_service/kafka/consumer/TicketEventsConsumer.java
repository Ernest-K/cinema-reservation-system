package com.example.reservation_service.kafka.consumer;

import com.example.reservation_service.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.example.commons.enums.ReservationStatus;
import org.example.commons.events.TicketValidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TicketEventsConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(TicketEventsConsumer.class);
    private final ReservationRepository reservationRepository;

    public static final String TICKET_VALIDATED_TOPIC_FOR_RESERVATION = "cinema.ticket.validated";

    @KafkaListener(topics = TICKET_VALIDATED_TOPIC_FOR_RESERVATION,
            groupId = "cinema-group-reservation",
            containerFactory = "ticketValidatedEventKafkaListenerContainerFactory")
    @Transactional
    public void handleTicketValidated(TicketValidatedEvent event) {
        LOG.info("Received TicketValidatedEvent for reservation ID: {}, ticket ID: {}",
                event.getReservationId(), event.getTicketId());

        reservationRepository.findById(event.getReservationId()).ifPresentOrElse(reservation -> {
            if (reservation.isTicketUsed()) {
                LOG.warn("Reservation ID: {} already marked as ticketUsed. Ignoring duplicate TicketValidatedEvent for ticket ID: {}",
                        reservation.getId(), event.getTicketId());
                return;
            }
            // Upewnij się, że rezerwacja jest w odpowiednim stanie (np. CONFIRMED)
            if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
                LOG.warn("TicketValidatedEvent received for reservation ID: {} which is not CONFIRMED (status: {}). This is unusual.",
                        reservation.getId(), reservation.getStatus());
                // Możesz zdecydować, czy mimo to oznaczyć bilet jako użyty,
                // czy zignorować/zalogować jako błąd.
            }

            reservation.setTicketUsed(true);
            reservation.setTicketUsedAt(event.getValidatedAt());
            reservationRepository.save(reservation);
            LOG.info("Reservation ID: {} marked as ticketUsed at {}.",
                    reservation.getId(), event.getValidatedAt());

        }, () -> LOG.warn("Reservation not found for ID: {} referenced in TicketValidatedEvent (ticket ID: {}).",
                event.getReservationId(), event.getTicketId()));
    }
}