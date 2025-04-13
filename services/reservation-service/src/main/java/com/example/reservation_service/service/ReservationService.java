package com.example.reservation_service.service;

import com.example.reservation_service.client.MovieServiceClient;
import com.example.reservation_service.dto.*;
import com.example.reservation_service.entity.Reservation;
import com.example.reservation_service.entity.ReservationStatus;
import com.example.reservation_service.entity.ReservedSeat;
import com.example.reservation_service.repository.ReservationRepository;
import com.example.reservation_service.repository.ReservedSeatRepository;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final MovieServiceClient movieServiceClient;

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

        List<SeatAvailabilityDTO> availabilityList = checkSeatsAvailability(
                request.getScreeningId(), request.getSeatIds());

        boolean allSeatsAvailable = availabilityList.stream()
                .allMatch(SeatAvailabilityDTO::isAvailable);

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

        return mapToReservationDTO(savedReservation, screening, seats);
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
}