package org.example.payment_service.reservation_service.controller;

import org.example.payment_service.reservation_service.dto.ReservationRequest;
import org.example.payment_service.reservation_service.dto.ReservationResponse;
import org.example.payment_service.reservation_service.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getReservations(@RequestParam Long userId) {
        List<ReservationResponse> responses = reservationService.getReservationsByUser(userId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.cancelReservation(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.confirmReservation(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/initiate-payment")
    public ResponseEntity<String> initiatePayment(@PathVariable Long id) {
        String paymentUrl = reservationService.initiatePayment(id);
        return ResponseEntity.ok(paymentUrl);
    }
    @PutMapping("/{id}/confirm-payment")
    public ResponseEntity<ReservationResponse> confirmPayment(@PathVariable Long id) {
        ReservationResponse response = reservationService.confirmReservation(id);
        return ResponseEntity.ok(response);
    }
}
