package com.example.movie_service.service;

import com.example.movie_service.dto.MovieResponse;
import com.example.movie_service.dto.RatingResponse;
import com.example.movie_service.entity.Movie;
import com.example.movie_service.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final RestTemplate restTemplate;

    public MovieService(MovieRepository movieRepository, RestTemplate restTemplate) {
        this.movieRepository = movieRepository;
        this.restTemplate = restTemplate;
    }

    public List<MovieResponse> getAllMoviesWithRatings() {
        List<Movie> movies = movieRepository.findAll();
        System.out.println(movies);
        return enrichWithRatings(movies);
    }

    public List<MovieResponse> getMoviesByTitleWithRatings(String title) {
        List<Movie> movies = movieRepository.findByTitleContaining(title);
        return enrichWithRatings(movies);
    }

    private List<MovieResponse> enrichWithRatings(List<Movie> movies) {
        return movies.stream()
                .map(movie -> new MovieResponse(
                        movie.getId(),
                        movie.getTitle(),
                        movie.getReleaseYear(),
                        fetchRatingFromExternalApi(movie.getTitle())
                ))
                .collect(Collectors.toList());
    }

    private double fetchRatingFromExternalApi(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String apiUrl = "http://www.omdbapi.com/?apikey=77e6892c&t=" + encodedTitle;

            RatingResponse response = restTemplate.getForObject(apiUrl, RatingResponse.class);
            return response != null ? parseRating(response.getImdbRating()) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double parseRating(String rating) {
        try {
            return Double.parseDouble(rating);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
