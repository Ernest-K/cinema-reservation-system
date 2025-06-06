package com.example.reservation_service.config;

import com.example.reservation_service.entity.ScreeningInfo;
import com.example.reservation_service.repository.ScreeningInfoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final ScreeningInfoRepository screeningInfoRepository;

    @PostConstruct
    private void init() {
        List<ScreeningInfo> screeningInfoList = null;

        screeningInfoList = List.of(
                new ScreeningInfo(1L, LocalDateTime.now(), new BigDecimal("20.00"), 1L, "The Shawshank Redemption", 1L, 1),
                new ScreeningInfo(2L, LocalDateTime.now().plus(3, ChronoUnit.HOURS), new BigDecimal("22.00"), 1L, "The Shawshank Redemption", 1L, 1),
                new ScreeningInfo(3L, LocalDateTime.now(), new BigDecimal("20.00"), 2L, "The Godfather", 2L, 2),
                new ScreeningInfo(4L, LocalDateTime.now(), new BigDecimal("20.00"), 3L, "Inception", 3L, 3)
                );

        screeningInfoRepository.saveAll(screeningInfoList);
    }
}