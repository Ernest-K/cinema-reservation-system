package com.example.movie_service.service;

import com.example.movie_service.dto.HallDTO;
import com.example.movie_service.dto.MovieDTO;
import com.example.movie_service.dto.ScreeningDTO;
import com.example.movie_service.dto.SeatDTO;
import com.example.movie_service.entity.Hall;
import com.example.movie_service.entity.Movie;
import com.example.movie_service.entity.Screening;
import com.example.movie_service.entity.Seat;
import com.example.movie_service.repository.ScreeningRepository;
import com.example.movie_service.repository.SeatRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;

    public List<ScreeningDTO> getScreeningsByMovieId(Long movieId) {
        return screeningRepository.findAllByMovieId(movieId).stream().map(this::mapToScreeningResponse).collect(Collectors.toList());
    }

    public ScreeningDTO getScreeningById(Long id) {
        return mapToScreeningResponse(screeningRepository.findById(id).orElseThrow(() -> new NotFoundException("Nie znaleziono seansu")));
    }

    public List<SeatDTO> getSeatsById(List<Long> ids) {
        return seatRepository.findAllById(ids).stream().map(this::mapToSeatResponse).collect(Collectors.toList());
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
