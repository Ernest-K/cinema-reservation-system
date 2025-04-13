package com.example.reservation_service.client;

import com.example.reservation_service.dto.ScreeningDTO;
import com.example.reservation_service.dto.SeatDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "movie-service", url = "${movie-service.url}")
public interface MovieServiceClient {

    @GetMapping("/api/movies/screenings/{screeningId}")
    ScreeningDTO getScreeningById(@PathVariable("screeningId") Long screeningId);

    @GetMapping("/api/movies/seats")
    List<SeatDTO> getSeatsById(@RequestParam("ids") List<Long> seatIds);
}