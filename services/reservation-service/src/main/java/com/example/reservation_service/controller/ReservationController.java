package com.example.reservation_service.controller;

import com.example.reservation_service.dto.CreateReservationDTO;
import com.example.reservation_service.dto.ReservationDTO;
import com.example.reservation_service.dto.SeatDTO;
import com.example.reservation_service.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @GetMapping("/screenings/{screeningId}/seats/availability")
    public List<SeatDTO> getReservedSeatsByScreeningId(@PathVariable("screeningId") Long screeningId) {
        return reservationService.getReservedSeatsByScreeningId(screeningId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDTO createReservation(@RequestBody CreateReservationDTO request) {
        return reservationService.createReservation(request);
    }

    @GetMapping("/{id}")
    public ReservationDTO getReservation(@PathVariable("id") Long id) {
        return reservationService.getReservation(id);
    }
}