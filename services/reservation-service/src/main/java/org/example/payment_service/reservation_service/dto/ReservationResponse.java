package org.example.payment_service.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private Long userId;
    private String movieTitle;
    private LocalDateTime screeningTime;
    private String seats;
    private LocalDateTime reservationDate;
    private String status;
}
