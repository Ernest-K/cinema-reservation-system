package org.example.payment_service.reservation_service.service;

import org.example.payment_service.reservation_service.dto.ReservationRequest;
import org.example.payment_service.reservation_service.dto.ReservationResponse;
import org.example.payment_service.reservation_service.entity.Reservation;
import org.example.payment_service.reservation_service.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public ReservationResponse createReservation(ReservationRequest request) {
        List<String> requestedSeats = Arrays.stream(request.getSeats().split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        List<Reservation> conflictingReservations = reservationRepository
                .findConflictingReservations(
                        request.getMovieTitle(),
                        request.getScreeningTime(),
                        LocalDateTime.now()
                );

        for (Reservation existing : conflictingReservations) {
            List<String> existingSeats = Arrays.stream(existing.getSeats().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            for (String seat : requestedSeats) {
                if (existingSeats.contains(seat)) {
                    throw new RuntimeException("Miejsce " + seat + " jest już zarezerwowane.");
                }
            }
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(request.getUserId());
        reservation.setMovieTitle(request.getMovieTitle());
        reservation.setScreeningTime(request.getScreeningTime());
        reservation.setSeats(request.getSeats()); // Przechowujemy oryginalny ciąg
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setStatus("tymczasowa");
        reservation.setBlockedUntil(LocalDateTime.now().plusMinutes(5));

        Reservation saved = reservationRepository.save(reservation);
        return mapToResponse(saved);
    }

    public List<ReservationResponse> getReservationsByUser(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        return reservations.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ReservationResponse cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Rezerwacja nie znaleziona"));
        reservation.setStatus("anulowana");
        Reservation saved = reservationRepository.save(reservation);
        return mapToResponse(saved);
    }

    public ReservationResponse confirmReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Rezerwacja nie znaleziona"));
        reservation.setStatus("zatwierdzona");
        Reservation saved = reservationRepository.save(reservation);
        return mapToResponse(saved);
    }


    public String initiatePayment(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Rezerwacja nie znaleziona"));
        if (!"tymczasowa".equals(reservation.getStatus())) {
            throw new RuntimeException("Rezerwacja nie jest w stanie tymczasowym");
        }
        return "https://dummy-payment-gateway.com/pay?reservationId=" + reservationId;
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getMovieTitle(),
                reservation.getScreeningTime(),
                reservation.getSeats(),
                reservation.getReservationDate(),
                reservation.getStatus()
        );
    }
}
