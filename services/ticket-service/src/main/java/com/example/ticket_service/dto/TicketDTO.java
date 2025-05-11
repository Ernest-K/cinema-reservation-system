package com.example.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private Long id;
    private Long reservationId;
    private Long screeningId;
    private String movieTitle;
    private LocalDateTime screeningStartTime;
    private int hallNumber;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalPrice;
    private String seatsDescription;
    private String qrCodeData;
    private LocalDateTime createdAt;
    private LocalDateTime validatedAt;
}
