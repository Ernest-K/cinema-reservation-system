package com.example.movie_service.controller;

import com.example.movie_service.dto.MovieResponse;
import com.example.movie_service.service.MovieService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<MovieResponse> getAllMovies() {
        return movieService.getAllMoviesWithRatings();
    }

    @GetMapping("/search")
    public List<MovieResponse> searchMovies(@RequestParam String title) {
        return movieService.getMoviesByTitleWithRatings(title);
    }
}
