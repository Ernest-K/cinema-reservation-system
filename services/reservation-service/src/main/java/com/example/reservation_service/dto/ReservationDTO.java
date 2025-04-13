package com.example.reservation_service.dto;

import com.example.reservation_service.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {
    private Long id;
    private ScreeningDTO screeningDTO;
    private String customerName;
    private String customerEmail;
    private LocalDateTime reservationTime;
    private ReservationStatus status;
    private BigDecimal totalAmount;
    private List<SeatDTO> seats;
}
