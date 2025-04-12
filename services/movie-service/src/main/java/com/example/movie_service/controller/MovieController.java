package com.example.movie_service.controller;

import com.example.movie_service.dto.MovieResponse;
import com.example.movie_service.dto.ScreeningResponse;
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
    public List<MovieResponse> getAllMovies() {
        return movieService.getAllMoviesWithRatings();
    }

    @GetMapping("/search")
    public List<MovieResponse> searchMovies(@RequestParam("title") String title) {
        return movieService.getMoviesByTitleWithRatings(title);
    }

    @GetMapping("/{movieId}/screenings")
    public List<ScreeningResponse> getScreeningsByMovieId(@PathVariable("movieId") Long movieId) {
        return screeningService.getScreeningsByMovieId(movieId);
    }
}
