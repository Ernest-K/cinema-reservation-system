package com.example.payment_service.dto;


import com.example.payment_service.dto.HallDTO;
import com.example.payment_service.dto.MovieDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningDTO {
    private Long id;
    private LocalDateTime startTime;
    private BigDecimal basePrice;
    private MovieDTO movieDTO;
    private HallDTO hallDTO;
}
