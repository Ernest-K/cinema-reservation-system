package org.example.payment_service.reservation_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String movieTitle;
    private LocalDateTime screeningTime;
    private String seats;
    private LocalDateTime reservationDate;
    private String status;
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;


}
