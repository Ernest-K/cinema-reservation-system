package com.example.reservation_service.service;

import com.example.reservation_service.client.MovieServiceClient;
import com.example.reservation_service.entity.Reservation;
import com.example.reservation_service.entity.ReservedSeat;
import com.example.reservation_service.kafka.producer.MessageProducer;
import com.example.reservation_service.repository.ReservationRepository;
import com.example.reservation_service.repository.ReservedSeatRepository;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.*;
import org.example.commons.enums.ReservationStatus;
import org.example.commons.events.PaymentFailedEvent;
import org.example.commons.events.ReservationCancelledEvent;
import org.example.commons.events.TicketGenerationFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private static final Logger LOG = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final MovieServiceClient movieServiceClient;
    private final MessageProducer messageProducer;

    public List<SeatDTO> getReservedSeatsByScreeningId(Long screeningId) {
        return reservedSeatRepository.findByReservation_ScreeningId(screeningId).stream().map(this::mapToSeatDTO).collect(Collectors.toList());
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
        ScreeningDTO screening = movieServiceClient.getScreeningById(request.getScreeningId());

        List<SeatAvailabilityDTO> availabilityList = checkSeatsAvailability(request.getScreeningId(), request.getSeatIds());

        boolean allSeatsAvailable = availabilityList.stream().allMatch(SeatAvailabilityDTO::isAvailable);

        if (!allSeatsAvailable) {
            throw new RuntimeException("Selected seats are not available");
        }

        Reservation reservation = new Reservation();
        reservation.setScreeningId(request.getScreeningId());
        reservation.setCustomerName(request.getCustomerName());
        reservation.setCustomerEmail(request.getCustomerEmail());
        reservation.setReservationTime(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.PENDING_PAYMENT);

        BigDecimal pricePerSeat = screening.getBasePrice();
        reservation.setTotalAmount(pricePerSeat.multiply(new BigDecimal(request.getSeatIds().size())));

        List<SeatDTO> seats = movieServiceClient.getSeatsById(request.getSeatIds());

        for (SeatDTO seatDTO : seats) {
            ReservedSeat seat = new ReservedSeat();
            seat.setSeatId(seatDTO.getId());
            seat.setRowNumber(seatDTO.getRowNumber());
            seat.setSeatNumber(seatDTO.getSeatNumber());
            reservation.getSeats().add(seat);
        }

        Reservation savedReservation = reservationRepository.save(reservation);

        ReservationDTO event = mapToReservationDTO(savedReservation, screening, seats);

        messageProducer.sendReservation(event);

        return event;
    }

    public ReservationDTO getReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        ScreeningDTO screening = movieServiceClient.getScreeningById(reservation.getScreeningId());

        List<SeatDTO> seats = reservation.getSeats().stream()
                .map(this::mapToSeatDTO)
                .collect(Collectors.toList());

        return mapToReservationDTO(reservation, screening, seats);
    }

    @Transactional
    public void updateReservationStatus(PaymentStatusDTO paymentStatusDTO) {
        Reservation reservation = reservationRepository.findById(paymentStatusDTO.getReservationId())
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        if ("completed".equals(paymentStatusDTO.getStatus())) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            ticketGenerationRequest(reservation);
        } else if("expired".equals(paymentStatusDTO.getStatus())) {
            reservation.getSeats().clear();
            reservation.setStatus(ReservationStatus.EXPIRED);
        } else {
            reservation.getSeats().clear();
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);

            messageProducer.sendReservationCancelled(new ReservationCancelledEvent(paymentStatusDTO.getReservationId()));
        }

        reservationRepository.save(reservation);
    }

    @Transactional
    public void handlePaymentGenerationFailure(PaymentFailedEvent event) {
        Reservation reservation = reservationRepository.findById(event.getReservationId())
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            LOG.info("Reservation {} already cancelled. Skipping compensation.", event.getReservationId());
            return;
        }

        LOG.info("Ticket generation failed for reservation {}. Initiating compensation.", event.getReservationId());
        reservation.getSeats().clear();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void handleTicketGenerationFailure(TicketGenerationFailedEvent event) {
        Reservation reservation = reservationRepository.findById(event.getReservationId())
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            LOG.info("Reservation {} already cancelled. Skipping compensation.", event.getReservationId());
            return;
        }

        LOG.info("Ticket generation failed for reservation {}. Initiating compensation.", event.getReservationId());
        reservation.getSeats().clear();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // Publikuj zdarzenie kompensacyjne, aby np. anulować płatność
        messageProducer.sendReservationCancelled(new ReservationCancelledEvent(event.getReservationId()));
    }

    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        reservation.getSeats().clear();
        reservation.setStatus(ReservationStatus.CANCELLED);

        reservationRepository.save(reservation);
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
        ReservationDTO reservationDTO = mapToReservationDTO(reservation, movieServiceClient.getScreeningById(reservation.getScreeningId()), reservation.getSeats().stream()
                .map(this::mapToSeatDTO)
                .collect(Collectors.toList()));

        messageProducer.sendTicketRequest(reservationDTO);
    }
}
