package com.example.movie_service.controller;

import com.example.movie_service.dto.MovieRatingDTO;
import com.example.movie_service.dto.ScreeningDTO;
import com.example.movie_service.dto.SeatDTO;
import com.example.movie_service.service.MovieService;
import com.example.movie_service.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;
    private final ScreeningService screeningService;

    @GetMapping
    public List<MovieRatingDTO> getAllMovies() {
        return movieService.getAllMoviesWithRatings();
    }

    @GetMapping("/search")
    public List<MovieRatingDTO> searchMovies(@RequestParam("title") String title) {
        return movieService.getMoviesByTitleWithRatings(title);
    }

    @GetMapping("/{movieId}/screenings")
    public List<ScreeningDTO> getScreeningsByMovieId(@PathVariable("movieId") Long movieId) {
        return screeningService.getScreeningsByMovieId(movieId);
    }

    @GetMapping("/screenings/{screeningId}")
    public ScreeningDTO getScreeningsById(@PathVariable("screeningId") Long screeningId) {
        return screeningService.getScreeningById(screeningId);
    }

    @GetMapping("/seats")
    public List<SeatDTO> getSeatsById(@RequestParam("ids") List<Long> seatIds) {
        return screeningService.getSeatsById(seatIds);
    }
}
