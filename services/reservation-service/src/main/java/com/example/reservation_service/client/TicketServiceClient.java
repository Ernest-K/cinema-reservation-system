package com.example.reservation_service.client;

import org.example.commons.dto.TicketDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ticket-service", url = "${ticket-service.url:http://localhost:8084}")
public interface TicketServiceClient {

    @GetMapping("/api/tickets/reservation/{reservationId}")
    TicketDTO getTicketByReservationId(@PathVariable("reservationId") Long reservationId);
}