package com.example.movie_service.config;

import com.example.movie_service.entity.Hall;
import com.example.movie_service.entity.Movie;
import com.example.movie_service.entity.Screening;
import com.example.movie_service.repository.HallRepository;
import com.example.movie_service.repository.MovieRepository;
import com.example.movie_service.repository.ScreeningRepository;
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

    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final ScreeningRepository screeningRepository;

    @PostConstruct
    private void init() {
        List<Movie> movies = null;
        List<Hall> halls = null;
        List<Screening> screenings = null;

        if (movieRepository.count() == 0) {
            movies = List.of(
                    new Movie("The Shawshank Redemption", 1994),
                    new Movie("The Godfather", 1972),
                    new Movie("Inception", 2010)
            );

            movieRepository.saveAll(movies);
        }

        if (hallRepository.count() == 0) {
            halls = List.of(
                    new Hall(1, 10, 15),
                    new Hall(2, 10, 15),
                    new Hall(3, 10, 15)
            );

            hallRepository.saveAll(halls);
        }

        if (screeningRepository.count() == 0) {
            screenings = List.of(
                    new Screening(LocalDateTime.now(), new BigDecimal("20.00"), movies.get(0), halls.get(0)),
                    new Screening(LocalDateTime.now(), new BigDecimal("30.00"), movies.get(1), halls.get(1)),
                    new Screening(LocalDateTime.now(), new BigDecimal("25.00"), movies.get(2), halls.get(2))
            );

            screeningRepository.saveAll(screenings);
        }
    }
}
