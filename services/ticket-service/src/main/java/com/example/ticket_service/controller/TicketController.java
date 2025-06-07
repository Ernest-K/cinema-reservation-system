package com.example.ticket_service.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.commons.dto.TicketDTO;
import com.example.ticket_service.service.TicketService;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @GetMapping("/reservation/{reservationId}")
    public TicketDTO getTicketByReservationId(
            @PathVariable("reservationId")
            @NotNull(message = "Reservation ID cannot be null.")
            @Positive(message = "Reservation ID must be a positive number.")
            Long reservationId) {
        return ticketService.getTicketByReservationId(reservationId);
    }

    @GetMapping("/{ticketId}")
    public TicketDTO getTicketByUid(
            @PathVariable("ticketId")
            @NotNull(message = "Ticket ID cannot be null.")
            @Positive(message = "Ticket ID must be a positive number.")
            Long ticketId) {
        return ticketService.getTicketById(ticketId);
    }

    @PostMapping("/{ticketId}/validate")
    public TicketDTO validateTicket(@PathVariable("ticketId") Long ticketId) {
        return ticketService.validateTicket(ticketId);
    }

    @GetMapping(value = "/{ticketId}/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getTicketQrCodeImage(
            @PathVariable("ticketId")
            @NotNull(message = "Ticket ID cannot be null.")
            @Positive(message = "Ticket ID must be a positive number.")
            Long ticketId) {
        try {
            byte[] qrImageBytes = ticketService.getQrCodeImageForTicket(ticketId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(qrImageBytes.length);

            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename("qr_ticket_" + ticketId + ".png")
                    .build();
            headers.setContentDisposition(contentDisposition);

            return new ResponseEntity<>(qrImageBytes, headers, HttpStatus.OK);

        } catch (IllegalStateException | IOException | WriterException e) {
            throw new RuntimeException("Failed to generate QR code image for ticket " + ticketId, e);
        }
    }

    @GetMapping(value = "/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getTicketQrCodeByEmailAndReservationId(
            @RequestParam("email") String email,
            @RequestParam("reservationId") Long reservationId) {

        try {
            byte[] qrImageBytes = ticketService.getQrCodeImageByEmailAndReservationId(email, reservationId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(qrImageBytes.length);

            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename("qr_ticket_" + reservationId + ".png")
                    .build();
            headers.setContentDisposition(contentDisposition);

            return new ResponseEntity<>(qrImageBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code image for reservation ID " + reservationId, e);
        }
    }

//    @PostMapping("/resend")
//    public ResponseEntity<String> resendTicketToEmail(@RequestParam("email") String email,
//                                                      @RequestParam("reservationId") Long reservationId) {
//        ticketService.resendTicketToEmail(email, reservationId);
//        return ResponseEntity.ok("Ticket has been resent to email: " + email);
//    }

    @PostMapping(value = "/regenerate", produces = MediaType.IMAGE_PNG_VALUE) // Nowa lub zmieniona ścieżka
    public ResponseEntity<byte[]> regenerateTicketAndGetQr(
            @RequestParam("email") @NotBlank @Email(message = "Valid email is required.") String email,
            @RequestParam("reservationId") @NotNull @Positive(message = "Reservation ID must be positive.") Long reservationId
    ) throws IOException, WriterException {
        // GlobalExceptionHandler obsłuży wyjątki
        byte[] qrImageBytes = ticketService.regenerateTicketAndGetQr(email, reservationId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(qrImageBytes.length);
        ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                .filename("regenerated_qr_ticket_reservation_" + reservationId + ".png")
                .build();
        headers.setContentDisposition(contentDisposition);

        return new ResponseEntity<>(qrImageBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/resend-by-email")
    public ResponseEntity<String> resendTicketByEmail(@RequestParam("email") String email) {
        ticketService.resendTicketByEmail(email);
        return ResponseEntity.ok("Ticket has been resent to: " + email);
    }

    @GetMapping("/uuid/{ticketUuid}")
    public TicketDTO getTicketByTicketUuid(@PathVariable String ticketUuid) {
        return ticketService.getTicketByUuid(ticketUuid); // Musisz dodać metodę getTicketByUuid do TicketService
    }

}
