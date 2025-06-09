package com.example.reservation_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "screening_info")
public class ScreeningInfo {
    @Id
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private Long movieId;

    @Column(nullable = false)
    private String movieTitle;

    @Column(nullable = false)
    private Long hallId;

    @Column(nullable = false)
    private int hallNumber;

    @Column(nullable = false)
    private int hallRows;

    @Column(nullable = false)
    private int hallSeatsPerRow;

    private boolean isActive = true;

    public ScreeningInfo(Long id, LocalDateTime startTime, BigDecimal basePrice, Long movieId, String movieTitle, Long hallId, int hallNumber) {
        this.id = id;
        this.startTime = startTime;
        this.basePrice = basePrice;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.hallId = hallId;
        this.hallNumber = hallNumber;
    }

    public ScreeningInfo(ScreeningInfo screeningInfo) {
        this.id = screeningInfo.id;
        this.startTime = screeningInfo.startTime;
        this.basePrice = screeningInfo.basePrice;
        this.movieId = screeningInfo.movieId;
        this.movieTitle = screeningInfo.movieTitle;
        this.hallId = screeningInfo.hallId;
        this.hallNumber = screeningInfo.hallNumber;
        this.isActive = screeningInfo.isActive;
    }

    public ScreeningInfo(Long id, LocalDateTime startTime, BigDecimal basePrice, Long movieId, String movieTitle, Long hallId, int hallNumber, int hallRows, int hallSeatsPerRow) {
        this.id = id;
        this.startTime = startTime;
        this.basePrice = basePrice;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.hallId = hallId;
        this.hallNumber = hallNumber;
        this.isActive = true; // Domyślnie aktywne
        this.hallRows = hallRows;
        this.hallSeatsPerRow = hallSeatsPerRow;
    }
}
