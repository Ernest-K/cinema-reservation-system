package com.example.ticket_service.controller;

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
}
