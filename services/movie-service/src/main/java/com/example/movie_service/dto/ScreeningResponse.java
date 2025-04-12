package com.example.movie_service.dto;

import com.example.movie_service.entity.Movie;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningResponse {
    private Long id;
    private LocalDateTime startTime;
    private BigDecimal basePrice;
    private Movie movie;
}
