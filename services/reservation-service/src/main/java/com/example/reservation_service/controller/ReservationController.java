package com.example.reservation_service.controller;

import com.example.reservation_service.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.example.commons.dto.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Validated
public class ReservationController {
    private final ReservationService reservationService;

    @GetMapping("/screenings/{screeningId}/seats/availability")
    public List<SeatDTO> getReservedSeatsByScreeningId(
            @PathVariable("screeningId")
            @NotNull(message = "Screening ID cannot be null.")
            @Positive(message = "Screening ID must be a positive number.")
            Long screeningId) {
        return reservationService.getReservedSeatsByScreeningId(screeningId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDTO createReservation(@Valid @RequestBody CreateReservationDTO request) {
        return reservationService.createReservation(request);
    }

    @GetMapping("/{id}")
    public ReservationDTO getReservation(
            @PathVariable("id")
            @NotNull(message = "Reservation ID cannot be null.")
            @Positive(message = "Reservation ID must be a positive number.")
            Long id) {
        return reservationService.getReservation(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(
            @PathVariable("id")
            @NotNull(message = "Reservation ID cannot be null.")
            @Positive(message = "Reservation ID must be a positive number.")
            Long id) {
        reservationService.cancelReservation(id);
    }
}