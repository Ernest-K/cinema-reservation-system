package org.example.payment_service.reservation_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReservationRequest {
    private Long userId;
    private String movieTitle;
    private LocalDateTime screeningTime;
    private String seats;
}
