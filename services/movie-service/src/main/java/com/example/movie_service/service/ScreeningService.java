package com.example.movie_service.service;

import org.example.commons.dto.*;
import com.example.movie_service.entity.Hall;
import com.example.movie_service.entity.Movie;
import com.example.movie_service.entity.Screening;
import com.example.movie_service.entity.Seat;
import com.example.movie_service.repository.ScreeningRepository;
import com.example.movie_service.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.example.commons.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ScreeningService {

    private static final Logger LOG = LoggerFactory.getLogger(ScreeningService.class);
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;

    public List<ScreeningDTO> getScreeningsByMovieId(Long movieId) {
        return screeningRepository.findAllByMovieId(movieId).stream().map(this::mapToScreeningResponse).collect(Collectors.toList());
    }

    public ScreeningDTO getScreeningById(Long id) {
        Screening screening = screeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening with ID " + id + " not found."));
        return mapToScreeningResponse(screening);    }

    public List<SeatDTO> getSeatsById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Seat IDs list cannot be null or empty.");
        }
        List<Seat> seats = seatRepository.findAllById(ids);
        if (seats.size() != ids.size()) {
            LOG.warn("Some seats not found for IDs: {}", ids);
        }
        return seats.stream().map(this::mapToSeatResponse).collect(Collectors.toList());
    }

    private ScreeningDTO mapToScreeningResponse(Screening screening) {
        return new ScreeningDTO(screening.getId(), screening.getStartTime(), screening.getBasePrice(), mapToMovieDTO(screening.getMovie()), mapToHallDTO(screening.getHall()));
    }

    private SeatDTO mapToSeatResponse(Seat seat) {
        return new SeatDTO(seat.getId(), seat.getRowNumber(), seat.getSeatNumber());
    }

    private MovieDTO mapToMovieDTO(Movie movie) {
        return new MovieDTO(movie.getId(), movie.getTitle());
    }

    private HallDTO mapToHallDTO(Hall hall) {
        return new HallDTO(hall.getId(), hall.getNumber(), hall.getRows(), hall.getSeatsPerRow());
    }
}
