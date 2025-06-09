package com.example.movie_service.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.commons.dto.*;
import com.example.movie_service.service.MovieService;
import com.example.movie_service.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/movies")
@Validated
public class MovieController {

    private final MovieService movieService;
    private final ScreeningService screeningService;

    @GetMapping
    public List<MovieRatingDTO> getAllMovies() {
        return movieService.getAllMoviesWithRatings();
    }

    @GetMapping("/search")

    public List<MovieRatingDTO> searchMovies(
            @RequestParam("title")
            @NotEmpty(message = "Search title cannot be empty.")
            @Size(min = 2, max = 100, message = "Search title must be between 2 and 100 characters.")
            String title) {
        return movieService.getMoviesByTitleWithRatings(title);
    }

    @GetMapping("/{movieId}/screenings")
    public List<ScreeningDTO> getScreeningsByMovieId(
            @PathVariable("movieId")
            @NotNull(message = "Movie ID cannot be null.")
            @Positive(message = "Movie ID must be a positive number.")
            Long movieId) {
        return screeningService.getScreeningsByMovieId(movieId);
    }

    @GetMapping("/screenings/{screeningId}")
    public ScreeningDTO getScreeningsById(
            @PathVariable("screeningId")
            @NotNull(message = "Screening ID cannot be null.")
            @Positive(message = "Screening ID must be a positive number.")
            Long screeningId) {
        return screeningService.getScreeningById(screeningId);
    }

    @GetMapping("/seats")
    public List<SeatDTO> getSeatsById(
            @RequestParam("ids")
            @NotEmpty(message = "Seat IDs list cannot be empty.")
            List<Long> seatIds) {
        return screeningService.getSeatsByIds(seatIds);
    }
}
