package com.example.movie_service.service;

import org.example.commons.dto.MovieRatingDTO;
import com.example.movie_service.dto.RatingDTO;
import com.example.movie_service.entity.Movie;
import com.example.movie_service.repository.MovieRepository;
import org.example.commons.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private static final Logger LOG = LoggerFactory.getLogger(ScreeningService.class);
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

    public List<MovieRatingDTO> getAllMoviesWithRatings() {
        List<Movie> movies = movieRepository.findAll();
        if (movies.isEmpty()) {
            return Collections.emptyList();
        }
        return enrichWithRatings(movies);
    }

    public List<MovieRatingDTO> getMoviesByTitleWithRatings(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Search title cannot be empty.");
        }
        List<Movie> movies = movieRepository.findByTitleContainingIgnoreCase(title);
        if (movies.isEmpty()) {
            return Collections.emptyList();
        }
        return enrichWithRatings(movies);
    }

    private List<MovieRatingDTO> enrichWithRatings(List<Movie> movies) {
        return movies.stream()
                .map(this::mapToMovieResponseWithRating)
                .collect(Collectors.toList());
    }

    private MovieRatingDTO mapToMovieResponseWithRating(Movie movie) {
        double rating = 0.0;
        try {
            rating = fetchRatingFromExternalApi(movie.getTitle());
        } catch (ExternalServiceException e) {
            LOG.warn("Could not fetch rating for movie '{}': {}. Using default rating.", movie.getTitle(), e.getMessage());
        }

        return new MovieRatingDTO(
                movie.getId(),
                movie.getTitle(),
                movie.getReleaseYear(),
                rating
        );
    }

    private double fetchRatingFromExternalApi(String title) {
        String encodedTitle;
        try {
            encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LOG.error("UTF-8 encoding not supported, this should not happen.", e);
            throw new ExternalServiceException("Failed to encode movie title due to unsupported encoding.", e);
        }

        String url = ratingsApiUrl + "?apikey=" + ratingsApiKey + "&t=" + encodedTitle;
        ResponseEntity<RatingDTO> responseEntity;

        try {
            responseEntity = restTemplate.getForEntity(url, RatingDTO.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                RatingDTO responseBody = responseEntity.getBody();
                if (responseBody != null && "True".equalsIgnoreCase(responseBody.getResponse())) {
                    return parseRating(responseBody.getImdbRating());
                } else if (responseBody != null) {
                    LOG.warn("OMDB API could not find movie titled '{}'. Response: {}", title, responseBody.toString());
                    return 0.0;
                } else {
                    LOG.warn("OMDB API returned 200 OK but with null body for title '{}'", title);
                    return 0.0;
                }
            } else {
                LOG.warn("OMDB API returned non-OK status: {} for title '{}'", responseEntity.getStatusCode(), title);
                throw new ExternalServiceException("External rating service returned HTTP status: " + responseEntity.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            LOG.error("HTTP error fetching rating for title '{}'. Status: {}, Response: {}",
                    title, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ExternalServiceException("HTTP error from external rating service: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            LOG.error("Error connecting to external rating service for title '{}': {}", title, e.getMessage(), e);
            throw new ExternalServiceException("Communication error with external rating service.", e);
        }
    }

    private double parseRating(String ratingStr) {
        if (ratingStr == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(ratingStr);
        } catch (NumberFormatException e) {
            LOG.warn("Could not parse rating string: '{}'", ratingStr, e);
            return 0.0;
        }
    }
}
