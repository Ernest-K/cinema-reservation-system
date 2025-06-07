package org.example.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodePayloadDTO {
    private Long ticketId;
    private String ticketUuid;
    private Long reservationId;
    private String movieTitle;
    private LocalDateTime screeningTime;
    private String hallInfo; // Np. "Sala 5"
    private List<String> seatsInfo; // Lista informacji o miejscach, np. ["Rząd 10, Miejsce 5", "Rząd 10, Miejsce 6"]
    private String customerName;
    private int numberOfSeats; // Ile miejsc obejmuje ten bilet
}