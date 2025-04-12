package com.example.movie_service.service;

import com.example.movie_service.dto.MovieResponse;
import com.example.movie_service.dto.RatingResponse;
import com.example.movie_service.entity.Movie;
import com.example.movie_service.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final RestTemplate restTemplate;

    @Value("${ratings.api.url}")
    private String ratingsApiUrl;

    @Value("${ratings.api.key}")
    private String ratingsApiKey;

    public MovieService(MovieRepository movieRepository, RestTemplate restTemplate) {
        this.movieRepository = movieRepository;
        this.restTemplate = restTemplate;
    }

    public List<MovieResponse> getAllMoviesWithRatings() {
        List<Movie> movies = movieRepository.findAll();
        return enrichWithRatings(movies);
    }

    public List<MovieResponse> getMoviesByTitleWithRatings(String title) {
        List<Movie> movies = movieRepository.findByTitleContainingIgnoreCase(title);
        return enrichWithRatings(movies);
    }

    private List<MovieResponse> enrichWithRatings(List<Movie> movies) {
        return movies.stream()
                .map(this::mapToMovieResponseWithRating)
                .collect(Collectors.toList());
    }

    private MovieResponse mapToMovieResponseWithRating(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getReleaseYear(),
                fetchRatingFromExternalApi(movie.getTitle())
        );
    }

    private double fetchRatingFromExternalApi(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String url = ratingsApiUrl + "?apikey=" + ratingsApiKey + "&t=" + encodedTitle;
            ResponseEntity<RatingResponse> responseEntity = restTemplate.getForEntity(url, RatingResponse.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                RatingResponse response = responseEntity.getBody();
                return response != null ? parseRating(response.getImdbRating()) : 0.0;
            } else {
                return 0.0;
            }
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
