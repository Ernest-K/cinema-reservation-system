package com.example.ticket_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;
    private Long screeningId;

    private String movieTitle; // Denormalizowane dla łatwiejszego dostępu
    private LocalDateTime screeningStartTime; // Denormalizowane

    private Long hallId;
    private int hallNumber; // Denormalizowane

    private Long seatId;
    private int seatRow;
    private int seatNumber;

    private String customerName;
    private String customerEmail;

    private BigDecimal price;
    private String seatsDescription;

    @Lob
    private String qrCodeData;

    private LocalDateTime validatedAt;
    private LocalDateTime createdAt;
}
