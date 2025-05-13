package com.example.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrCodePayload {
    private Long ticketId;
    private Long reservationId;
    private String movieTitle;
    private LocalDateTime screeningTime;
    private String hallInfo; // Np. "Sala 5"
    private List<String> seatsInfo; // Lista informacji o miejscach, np. ["Rząd 10, Miejsce 5", "Rząd 10, Miejsce 6"]
    private String customerName;
    private int numberOfSeats; // Ile miejsc obejmuje ten bilet
}