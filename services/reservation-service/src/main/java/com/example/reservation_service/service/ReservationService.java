package com.example.reservation_service.service;

import com.example.reservation_service.client.MovieServiceClient;
import com.example.reservation_service.client.TicketServiceClient;
import com.example.reservation_service.entity.Reservation;
import com.example.reservation_service.entity.ReservedSeat;
import com.example.reservation_service.entity.ScreeningInfo;
import com.example.reservation_service.kafka.producer.MessageProducer;
import com.example.reservation_service.repository.ReservationRepository;
import com.example.reservation_service.repository.ReservedSeatRepository;
import com.example.reservation_service.repository.ScreeningInfoRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.*;
import org.example.commons.enums.PaymentStatus;
import org.example.commons.enums.ReservationStatus;
import org.example.commons.enums.TicketStatus;
import org.example.commons.events.PaymentFailedEvent;
import org.example.commons.events.ReservationCancelledEvent;
import org.example.commons.events.ScreeningUpdatedEvent;
import org.example.commons.events.TicketGenerationFailedEvent;
import org.example.commons.exception.InvalidReservationStatusException;
import org.example.commons.exception.ReservationConflictException;
import org.example.commons.exception.ResourceNotFoundException;
import org.example.commons.exception.ServiceCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private static final Logger LOG = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository reservationRepository;
    private final ScreeningInfoRepository screeningInfoRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final MovieServiceClient movieServiceClient;
    private final TicketServiceClient ticketServiceClient;
    private final MessageProducer messageProducer;

    public List<SeatDTO> getReservedSeatsByScreeningId(Long screeningId) {
        List<ReservedSeat> reservedSeats = reservedSeatRepository.findByReservation_ScreeningId(screeningId);
        return reservedSeats.stream().map(this::mapToSeatDTO).collect(Collectors.toList());
    }

    private List<SeatAvailabilityDTO> checkSeatsAvailability(Long screeningId, List<Long> seatIds) {
        return seatIds.stream()
                .map(seatId -> {
                    boolean isAvailable = !reservedSeatRepository.existsBySeatIdAndReservation_ScreeningIdAndReservation_StatusNot(
                            seatId, screeningId, ReservationStatus.CANCELLED);
                    return new SeatAvailabilityDTO(seatId, isAvailable);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationDTO createReservation(CreateReservationDTO request) {
        LOG.info("Attempting to create reservation for screening ID: {} by customer: {}", request.getScreeningId(), request.getCustomerEmail());

        ScreeningDTO screening;
        List<SeatDTO> seats;

        try {
            LOG.debug("Fetching screening details for ID: {}", request.getScreeningId());
            screening = movieServiceClient.getScreeningById(request.getScreeningId());
            if (screening == null) { // Dodatkowe zabezpieczenie, Feign powinien rzucić wyjątek przy 404
                throw new ResourceNotFoundException("Screening with ID " + request.getScreeningId() + " not found via movie-service.");
            }
            LOG.debug("Fetching seat details for IDs: {}", request.getSeatIds());
            seats = movieServiceClient.getSeatsById(request.getSeatIds());
            if (seats == null || seats.size() != request.getSeatIds().size()) {
                throw new ResourceNotFoundException("Could not find all requested seats via movie-service. Requested: " + request.getSeatIds().size() + ", Found: " + (seats != null ? seats.size() : 0));
            }
        } catch (FeignException e) {
            LOG.error("Error communicating with movie-service while creating reservation. Status: {}, URL: {}, Message: {}",
                    e.status(), e.request() != null ? e.request().url() : "N/A", e.contentUTF8(), e);
            throw new ServiceCommunicationException("Failed to retrieve screening or seat details from movie-service.", e);
        } catch (Exception e) {
            LOG.error("Unexpected error communicating with movie-service: {}", e.getMessage(), e);
            throw new ServiceCommunicationException("Unexpected error communicating with movie-service.", e);
        }

        List<SeatAvailabilityDTO> availabilityList = checkSeatsAvailability(request.getScreeningId(), request.getSeatIds());
        List<SeatAvailabilityDTO> unavailableSeats = availabilityList.stream().filter(s -> !s.isAvailable()).collect(Collectors.toList());

        if (!unavailableSeats.isEmpty()) {
            String unavailableSeatIds = unavailableSeats.stream()
                    .map(s -> s.getSeatId().toString())
                    .collect(Collectors.joining(", "));
            LOG.warn("Attempt to reserve unavailable seats. Screening ID: {}, Unavailable Seat IDs: {}", request.getScreeningId(), unavailableSeatIds);
            throw new ReservationConflictException("Selected seats are not available. Unavailable seat IDs: " + unavailableSeatIds);
        }

        Reservation reservation = new Reservation();
        reservation.setScreeningId(request.getScreeningId());
        reservation.setCustomerName(request.getCustomerName());
        reservation.setCustomerEmail(request.getCustomerEmail());
        reservation.setReservationTime(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.PENDING_PAYMENT);

        BigDecimal pricePerSeat = screening.getBasePrice();
        if (pricePerSeat == null || pricePerSeat.compareTo(BigDecimal.ZERO) <= 0) {
            LOG.error("Invalid base price ({}) for screening ID: {}", pricePerSeat, screening.getId());
            throw new IllegalStateException("Screening base price is invalid.");
        }
        reservation.setTotalAmount(pricePerSeat.multiply(new BigDecimal(request.getSeatIds().size())));

        for (SeatDTO seatDTO : seats) {
            ReservedSeat seat = ReservedSeat.builder()
                    .seatId(seatDTO.getId())
                    .rowNumber(seatDTO.getRowNumber())
                    .seatNumber(seatDTO.getSeatNumber())
                    .reservation(reservation)
                    .build();
            reservation.getSeats().add(seat);
        }

        Reservation savedReservation = reservationRepository.save(reservation);
        LOG.info("Reservation created successfully with ID: {}", savedReservation.getId());

        ReservationDTO event = mapToReservationDTO(savedReservation, screening, seats);
        messageProducer.sendReservation(event);
        return event;
    }

    public ReservationDTO getReservation(Long reservationId) {
        LOG.debug("Fetching reservation with ID: {}", reservationId);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + reservationId + " not found."));

        ScreeningDTO screening;
        List<SeatDTO> seats;
        try {
            screening = movieServiceClient.getScreeningById(reservation.getScreeningId());
            List<Long> seatIds = reservation.getSeats().stream().map(ReservedSeat::getSeatId).collect(Collectors.toList());
            if (!seatIds.isEmpty()) {
                seats = movieServiceClient.getSeatsById(seatIds);
            } else {
                seats = Collections.emptyList();
            }

        } catch (FeignException e) {
            LOG.error("Error communicating with movie-service while fetching details for reservation ID: {}. Error: {}", reservationId, e.getMessage(), e);
            throw new ServiceCommunicationException("Failed to retrieve screening or seat details from movie-service for reservation " + reservationId, e);
        }

        return mapToReservationDTO(reservation, screening, seats);
    }

    @Transactional
    public void updateReservationStatus(PaymentStatusDTO paymentStatusDTO) {
        LOG.info("Updating reservation status for ID: {} based on payment status: {}", paymentStatusDTO.getReservationId(), paymentStatusDTO.getStatus());
        Reservation reservation = reservationRepository.findById(paymentStatusDTO.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + paymentStatusDTO.getReservationId() + " not found for status update."));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED && "completed".equals(paymentStatusDTO.getStatus())) {
            LOG.warn("Reservation {} is already confirmed. Ignoring duplicate 'completed' payment status.", reservation.getId());
            return;
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.EXPIRED) {
            LOG.warn("Reservation {} is already {} and cannot be changed by payment status {}.",
                    reservation.getId(), reservation.getStatus(), paymentStatusDTO.getStatus());
            return;
        }

        if (PaymentStatus.COMPLETED == paymentStatusDTO.getStatus()) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            LOG.info("Reservation ID: {} confirmed.", reservation.getId());
            ticketGenerationRequest(reservation);
        } else if (PaymentStatus.EXPIRED == paymentStatusDTO.getStatus()) {
            LOG.info("Payment for reservation ID: {} expired. Cancelling reservation.", reservation.getId());
            reservation.getSeats().clear();
            reservation.setStatus(ReservationStatus.EXPIRED);
        } else {
            LOG.warn("Payment for reservation ID: {} was not completed (status: {}). Cancelling reservation.", reservation.getId(), paymentStatusDTO.getStatus());
            reservation.getSeats().clear();
            reservation.setStatus(ReservationStatus.CANCELLED);

            ReservationCancelledEvent event = mapReservationCancelledEvent(reservation, "PAYMENT_ERROR");
            messageProducer.sendReservationCancelled(event);
        }

        reservationRepository.save(reservation);
    }

    @Transactional
    public void handlePaymentGenerationFailure(PaymentFailedEvent event) {
        LOG.warn("Handling payment generation failure for reservation ID: {}. Reason: {}", event.getReservationId(), event.getReason());
        Reservation reservation = reservationRepository.findById(event.getReservationId())
                .orElseGet(() -> {
                    LOG.error("Reservation not found for payment failure event (ID: {}). Cannot compensate.", event.getReservationId());
                    return null;
                });

        if (reservation == null) return;

        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.EXPIRED) {
            LOG.info("Reservation {} already {} on payment failure. No action needed.", event.getReservationId(), reservation.getStatus());
            return;
        }
        LOG.info("Payment generation failed for reservation {}. Setting status to CANCELLED.", event.getReservationId());
        reservation.getSeats().clear();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void handleTicketGenerationFailure(TicketGenerationFailedEvent event) {
        LOG.warn("Handling ticket generation failure for reservation ID: {}. Reason: {}", event.getReservationId(), event.getReason());
        Reservation reservation = reservationRepository.findById(event.getReservationId())
                .orElseGet(() -> {
                    LOG.error("Reservation not found for ticket failure event (ID: {}). Cannot compensate.", event.getReservationId());
                    return null;
                });

        if (reservation == null) return;

        // Jeśli rezerwacja była POTWIERDZONA, a bilet się nie wygenerował, to jest poważny problem.
        // Powinniśmy ją anulować i powiadomić o konieczności zwrotu środków/anulowania płatności.
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            LOG.error("CRITICAL: Ticket generation failed for a CONFIRMED reservation ID: {}. Reason: {}. Initiating cancellation and payment reversal.",
                    event.getReservationId(), event.getReason());

            reservation.getSeats().clear();
            reservation.setStatus(ReservationStatus.CANCELLED); // Oznacz jako anulowaną z powodu błędu
            reservationRepository.save(reservation);
            ReservationCancelledEvent cancelledEvent = mapReservationCancelledEvent(reservation, "TICKET_ERROR");
            messageProducer.sendReservationCancelled(cancelledEvent);
        } else if (reservation.getStatus() != ReservationStatus.CANCELLED && reservation.getStatus() != ReservationStatus.EXPIRED) {
            LOG.warn("Ticket generation failed for reservation {} in status {}. Setting to CANCELLED.",
                    event.getReservationId(), reservation.getStatus());

            reservation.getSeats().clear();
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
            ReservationCancelledEvent cancelledEvent = mapReservationCancelledEvent(reservation, "TICKET_ERROR");
            messageProducer.sendReservationCancelled(cancelledEvent);
        } else {
            LOG.info("Reservation {} already {}. No action needed for ticket generation failure.", event.getReservationId(), reservation.getStatus());
        }
    }

    @Transactional
    public void cancelReservation(Long reservationId) {
        LOG.info("Attempting to cancel reservation with ID: {}", reservationId);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + reservationId + " not found for cancellation."));

        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.EXPIRED) {
            LOG.warn("Reservation ID: {} is already {} and cannot be cancelled again.", reservationId, reservation.getStatus());
            throw new InvalidReservationStatusException("Reservation is already " + reservation.getStatus() + ".");
        }
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            LOG.warn("Attempting to cancel a CONFIRMED reservation ID: {}. This might require a refund process.", reservationId);
        }

        boolean ticketUsed = false;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            try {
                LOG.debug("Checking ticket status for reservation ID: {}", reservationId);
                TicketDTO ticket = ticketServiceClient.getTicketByReservationId(reservationId);
                if (ticket != null && ticket.getStatus() == TicketStatus.USED) {
                    ticketUsed = true;
                    LOG.info("Ticket for reservation ID {} was found and is USED.", reservationId);
                } else if (ticket != null) {
                    LOG.info("Ticket for reservation ID {} found with status: {}", reservationId, ticket.getStatus());
                } else {
                    LOG.warn("No ticket found by TicketService for confirmed reservation ID: {} during cancellation attempt. Proceeding with caution.", reservationId);
                }
            } catch (FeignException e) {
                if (e.status() == 404) {
                    LOG.warn("TicketService reported no ticket found (404) for reservation ID {} during cancellation.", reservationId);
                } else {
                    LOG.error("Error communicating with TicketService to check ticket status for reservation ID {}: {}. Status: {}",
                            reservationId, e.getMessage(), e.status(), e);
                }
            } catch (Exception e) {
                LOG.error("Unexpected error checking ticket status for reservation ID {}: {}", reservationId, e.getMessage(), e);
                // throw new ServiceCommunicationException("Unexpected error checking ticket status.", e);
            }
        }

        if (ticketUsed) {
            LOG.error("Cannot cancel reservation ID: {} because its ticket has already been USED.", reservationId);
            throw new ReservationConflictException("Cannot cancel reservation: ticket has already been used.");
        }

        reservation.getSeats().clear();
        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation cancelledReservation = reservationRepository.save(reservation);
        LOG.info("Reservation ID: {} cancelled successfully.", cancelledReservation.getId());

        ReservationCancelledEvent event = mapReservationCancelledEvent(reservation, "USER");
        messageProducer.sendReservationCancelled(event);
    }

    @Transactional
    public void handleScreeningCancellation(Long cancelledScreeningId, String reason) {
        LOG.info("Handling cancellation of screening ID: {}. Reason: {}", cancelledScreeningId, reason);

        // Pobierz szczegóły anulowanego seansu z lokalnej kopii
        ScreeningInfo cancelledScreeningCopy = screeningInfoRepository.findById(cancelledScreeningId)
                .orElse(null); // Lub rzuć wyjątek, jeśli kopia powinna istnieć

        if (cancelledScreeningCopy == null) {
            LOG.warn("ScreeningCopy not found for cancelledScreeningId: {}. Cannot send detailed notifications.", cancelledScreeningId);
            // Można próbować pobrać z movie-service, ale to łamie zasadę niezależności
        }

        List<Reservation> affectedReservations = reservationRepository.findAllByScreeningIdAndStatusIn(
                cancelledScreeningId,
                List.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.CONFIRMED)
        );

        if (affectedReservations.isEmpty()) {
            LOG.info("No active reservations found for cancelled screening ID: {}. No user notifications needed.", cancelledScreeningId);
            return;
        }

        LOG.info("Found {} active reservations for cancelled screening ID: {}. Proceeding to cancel them and notify users.",
                affectedReservations.size(), cancelledScreeningId);

        for (Reservation reservation : affectedReservations) {
            ReservationStatus oldStatus = reservation.getStatus();
            reservation.setStatus(ReservationStatus.CANCELLED);
            // Rozważ, czy usuwać `reservedSeats` - zależy od logiki biznesowej
            // Jeśli nie usuwasz, upewnij się, że nigdzie nie są one traktowane jako aktywne
            // List<ReservedSeat> seatsToClear = new ArrayList<>(reservation.getSeats());
            // reservation.getSeats().clear();
            // reservedSeatRepository.deleteAll(seatsToClear); // Jeśli cascade nie jest ustawiony lub chcesz być jawny

            reservationRepository.save(reservation);
            LOG.info("Reservation ID: {} (for screening ID: {}) status changed from {} to CANCELLED due to screening cancellation.",
                    reservation.getId(), cancelledScreeningId, oldStatus);

            // Publikuj event o anulowaniu rezerwacji dla innych serwisów (Payment, Ticket)
            ReservationCancelledEvent event = mapReservationCancelledEvent(reservation, "CANCELED_SCREENING");
            messageProducer.sendReservationCancelled(event);

            // Przygotuj i wyślij powiadomienie do użytkownika
            if (cancelledScreeningCopy != null) {
                ScreeningChangeNotificationDTO notificationPayload = ScreeningChangeNotificationDTO.builder()
                        .customerEmail(reservation.getCustomerEmail())
                        .customerName(reservation.getCustomerName())
                        .movieTitle(cancelledScreeningCopy.getMovieTitle()) // Z kopii seansu
                        .reservationId(reservation.getId())
                        .originalScreeningId(cancelledScreeningId)
                        .changeType("CANCELLED")
                        .changeReason(reason)
                        .oldScreeningTime(cancelledScreeningCopy.getStartTime()) // Z kopii seansu
                        .oldHallInfo("Hall " + cancelledScreeningCopy.getHallNumber()) // Z kopii seansu
                        .build();
                messageProducer.sendScreeningChangeNotification(notificationPayload); // Nowa metoda w MessageProducer
            } else {
                // Uproszczone powiadomienie, jeśli nie ma szczegółów seansu
                ScreeningChangeNotificationDTO notificationPayload = ScreeningChangeNotificationDTO.builder()
                        .customerEmail(reservation.getCustomerEmail())
                        .customerName(reservation.getCustomerName())
                        .reservationId(reservation.getId())
                        .originalScreeningId(cancelledScreeningId)
                        .changeType("CANCELLED")
                        .changeReason("The screening for your reservation has been cancelled. Please contact support for more details.")
                        .build();
                messageProducer.sendScreeningChangeNotification(notificationPayload);
            }
        }
    }

    @Transactional
    public void handleScreeningUpdate(ScreeningUpdatedEvent event, ScreeningInfo oldScreeningInfo) {
        LOG.info("Handling update of screening ID: {}.", event.getScreeningId());
        ScreeningDTO updatedDetails = event.getUpdatedScreeningDTO();

        List<Reservation> affectedReservations = reservationRepository.findAllByScreeningIdAndStatusIn(
                event.getScreeningId(),
                List.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.CONFIRMED) // Tylko aktywne rezerwacje
        );

        if (affectedReservations.isEmpty()) {
            LOG.info("No active reservations found for updated screening ID: {}. No user notifications needed for update.", event.getScreeningId());
            return;
        }

        LOG.info("Found {} active reservations for updated screening ID: {}. Notifying users.",
                affectedReservations.size(), event.getScreeningId());

        for (Reservation reservation : affectedReservations) {
            ScreeningChangeNotificationDTO notificationPayload = ScreeningChangeNotificationDTO.builder()
                    .customerEmail(reservation.getCustomerEmail())
                    .customerName(reservation.getCustomerName())
                    .movieTitle(updatedDetails.getMovieDTO().getTitle()) // Nowy tytuł filmu
                    .reservationId(reservation.getId())
                    .originalScreeningId(event.getScreeningId())
                    .changeType("UPDATED")
                    .changeReason("The screening details have been updated.") // Można dodać bardziej szczegółowy powód
                    .oldScreeningTime(oldScreeningInfo.getStartTime())
                    .oldHallInfo("Hall " + oldScreeningInfo.getHallNumber())
                    .newScreeningTime(updatedDetails.getStartTime())
                    .newHallInfo("Hall " + updatedDetails.getHallDTO().getNumber())
                    .build();
            messageProducer.sendScreeningChangeNotification(notificationPayload);
        }
    }

    private SeatDTO mapToSeatDTO(ReservedSeat reservedSeat) {
        return new SeatDTO(reservedSeat.getSeatId(), reservedSeat.getRowNumber(), reservedSeat.getSeatNumber());
    }

    private ReservationDTO mapToReservationDTO(Reservation reservation, ScreeningDTO screening, List<SeatDTO> seats) {
        return new ReservationDTO(
                reservation.getId(),
                screening,
                reservation.getCustomerName(),
                reservation.getCustomerEmail(),
                reservation.getReservationTime(),
                reservation.getStatus(),
                reservation.getTotalAmount(),
                seats
        );
    }

    public void ticketGenerationRequest(Reservation reservation) {
        LOG.info("Preparing ticket generation request for confirmed reservation ID: {}", reservation.getId());
        ScreeningDTO screening;
        List<SeatDTO> seats;
        try {
            screening = movieServiceClient.getScreeningById(reservation.getScreeningId());
            List<Long> seatIds = reservation.getSeats().stream().map(ReservedSeat::getSeatId).collect(Collectors.toList());
            if (!seatIds.isEmpty()) {
                seats = movieServiceClient.getSeatsById(seatIds);
            } else {
                seats = Collections.emptyList();
            }
        } catch (FeignException e) {
            LOG.error("Failed to fetch screening/seat details for ticket generation (Reservation ID: {}). Error: {}", reservation.getId(), e.getMessage(), e);
            throw new ServiceCommunicationException("Failed to prepare ticket request due to movie-service communication error.", e);
        }

        ReservationDTO reservationDTO = mapToReservationDTO(reservation, screening, seats);
        messageProducer.sendTicketRequest(reservationDTO);
        LOG.info("Ticket generation request sent for reservation ID: {}", reservation.getId());
    }

    public ReservationCancelledEvent mapReservationCancelledEvent(Reservation reservation, String reason) {
        Optional<ScreeningInfo> screeningDetails = screeningInfoRepository.findById(reservation.getScreeningId());
        if (screeningDetails.isEmpty()) {
            LOG.error("Could not fetch screening details for reservation {} during cancellation event creation. Event will have partial data.", reservation.getId());
        }
        return ReservationCancelledEvent.builder()
                .reservationId(reservation.getId())
                .customerEmail(reservation.getCustomerEmail())
                .customerName(reservation.getCustomerName())
                .movieTitle(screeningDetails.map(ScreeningInfo::getMovieTitle).orElse("N/A"))
                .screeningStartTime(screeningDetails.map(ScreeningInfo::getStartTime).orElse(null))
                .cancellationReason(reason)
                .build();
    }
}
