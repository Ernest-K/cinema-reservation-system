package com.example.ticket_service.controller;

import com.example.ticket_service.dto.TicketDTO;
import com.example.ticket_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @GetMapping("/reservation/{reservationId}")
    public TicketDTO getTicketByReservationId(@PathVariable("reservationId") Long reservationId) {
        return ticketService.getTicketByReservationId(reservationId);
    }

    @GetMapping("/{ticketId}")
    public TicketDTO getTicketByUid(@PathVariable("ticketId") Long ticketId) {
        return ticketService.getTicketById(ticketId);
    }

    @PostMapping("/{ticketId}/validate")
    public TicketDTO validateTicket(@PathVariable("ticketId") Long ticketId) {
        return ticketService.validateTicket(ticketId);
    }
}
