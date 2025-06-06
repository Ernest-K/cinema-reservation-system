package com.example.movie_service.service;

import com.example.movie_service.entity.Hall;
import com.example.movie_service.entity.Movie;
import com.example.movie_service.entity.Screening;
import com.example.movie_service.entity.Seat;
import com.example.movie_service.kafka.producer.ScreeningEventProducer; // Zaimportuj producenta
import com.example.movie_service.repository.HallRepository;
import com.example.movie_service.repository.MovieRepository;
import com.example.movie_service.repository.ScreeningRepository;
import com.example.movie_service.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.HallDTO; // Używamy DTO z commons
import org.example.commons.dto.MovieDTO;
import org.example.commons.dto.ScreeningDTO;
import org.example.commons.dto.SeatDTO;
import org.example.commons.events.ScreeningCancelledEvent; // Import eventów
import org.example.commons.events.ScreeningCreatedEvent;
import org.example.commons.events.ScreeningUpdatedEvent;
import org.example.commons.exception.ResourceNotFoundException; // Użyj wyjątku z commons
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private static final Logger LOG = LoggerFactory.getLogger(ScreeningService.class);

    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository; // Zmieniona nazwa
    private final SeatRepository seatRepository; // Zakładając, że jest potrzebne do mapowania HallDTO
    private final ScreeningEventProducer screeningEventProducer; // Wstrzyknij producenta

    @Transactional
    public ScreeningDTO createScreening(ScreeningDTO screeningRequestDTO) {
        LOG.info("Attempting to create screening for movie ID: {} in hall ID: {}",
                screeningRequestDTO.getMovieDTO() != null ? screeningRequestDTO.getMovieDTO().getId() : "null",
                screeningRequestDTO.getHallDTO() != null ? screeningRequestDTO.getHallDTO().getId() : "null");

        if (screeningRequestDTO.getMovieDTO() == null || screeningRequestDTO.getMovieDTO().getId() == null) {
            throw new IllegalArgumentException("Movie ID must be provided for creating a screening.");
        }
        if (screeningRequestDTO.getHallDTO() == null || screeningRequestDTO.getHallDTO().getId() == null) {
            throw new IllegalArgumentException("Hall ID must be provided for creating a screening.");
        }

        Movie movie = movieRepository.findById(screeningRequestDTO.getMovieDTO().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with ID: " + screeningRequestDTO.getMovieDTO().getId()));
        Hall hall = hallRepository.findById(screeningRequestDTO.getHallDTO().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with ID: " + screeningRequestDTO.getHallDTO().getId()));

        Screening newScreening = Screening.builder()
                .startTime(screeningRequestDTO.getStartTime())
                .basePrice(screeningRequestDTO.getBasePrice())
                .movie(movie)
                .hall(hall)
                .build();

        Screening savedScreening = screeningRepository.save(newScreening);
        LOG.info("Screening created successfully with ID: {}", savedScreening.getId());

        ScreeningDTO responseDTO = mapToScreeningDTO(savedScreening);
        screeningEventProducer.sendScreeningCreated(new ScreeningCreatedEvent(responseDTO));

        return responseDTO;
    }

    @Transactional(readOnly = true)
    public ScreeningDTO getScreeningDetailsById(Long id) {
        LOG.debug("Fetching screening details for ID: {}", id);
        Screening screening = screeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening not found with ID: " + id));
        return mapToScreeningDTO(screening);
    }

    @Transactional(readOnly = true)
    public List<ScreeningDTO> getScreeningsByMovieId(Long movieId) {
        LOG.debug("Fetching screenings for movie ID: {}", movieId);
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie not found with ID: " + movieId + ", cannot fetch screenings.");
        }
        List<Screening> screenings = screeningRepository.findAllByMovieId(movieId);
        return screenings.stream().map(this::mapToScreeningDTO).collect(Collectors.toList());
    }


    @Transactional
    public ScreeningDTO updateScreening(Long screeningId, ScreeningDTO screeningUpdateDTO) {
        LOG.info("Attempting to update screening with ID: {}", screeningId);

        Screening existingScreening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening not found with ID: " + screeningId + " for update."));

        // Aktualizuj tylko pola, które mogą się zmienić i są dostarczone
        if (screeningUpdateDTO.getStartTime() != null) {
            existingScreening.setStartTime(screeningUpdateDTO.getStartTime());
        }
        if (screeningUpdateDTO.getBasePrice() != null) {
            existingScreening.setBasePrice(screeningUpdateDTO.getBasePrice());
        }

        // Zmiana filmu lub sali dla istniejącego seansu może być bardziej skomplikowana
        // i wymagać dodatkowej logiki biznesowej (np. co z istniejącymi rezerwacjami?).
        // Na razie zakładamy, że film i sala nie są zmieniane przez ten endpoint,
        // lub jeśli są, to ID są dostarczane w DTO.
        if (screeningUpdateDTO.getMovieDTO() != null && screeningUpdateDTO.getMovieDTO().getId() != null) {
            if (!screeningUpdateDTO.getMovieDTO().getId().equals(existingScreening.getMovie().getId())) {
                Movie newMovie = movieRepository.findById(screeningUpdateDTO.getMovieDTO().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("New Movie not found with ID: " + screeningUpdateDTO.getMovieDTO().getId()));
                existingScreening.setMovie(newMovie);
            }
        }
        if (screeningUpdateDTO.getHallDTO() != null && screeningUpdateDTO.getHallDTO().getId() != null) {
            if (!screeningUpdateDTO.getHallDTO().getId().equals(existingScreening.getHall().getId())) {
                Hall newHall = hallRepository.findById(screeningUpdateDTO.getHallDTO().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("New Hall not found with ID: " + screeningUpdateDTO.getHallDTO().getId()));
                existingScreening.setHall(newHall);
            }
        }

        Screening updatedScreening = screeningRepository.save(existingScreening);
        LOG.info("Screening with ID: {} updated successfully.", updatedScreening.getId());

        ScreeningDTO responseDTO = mapToScreeningDTO(updatedScreening);
        screeningEventProducer.sendScreeningUpdated(new ScreeningUpdatedEvent(updatedScreening.getId(), responseDTO));

        return responseDTO;
    }

    @Transactional
    public void cancelScreening(Long screeningId, String reason) {
        LOG.info("Attempting to cancel screening with ID: {}, Reason: {}", screeningId, reason);
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening not found with ID: " + screeningId + " for cancellation."));
        
        screeningRepository.delete(screening);
        LOG.info("Screening with ID: {} deleted from database.", screeningId);

        screeningEventProducer.sendScreeningCancelled(new ScreeningCancelledEvent(screeningId, reason));
        LOG.info("ScreeningCancelledEvent published for screening ID: {}", screeningId);
    }


    private ScreeningDTO mapToScreeningDTO(Screening screening) {
        if (screening == null) return null;
        return new ScreeningDTO(
                screening.getId(),
                screening.getStartTime(),
                screening.getBasePrice(),
                mapToMovieDTO(screening.getMovie()),
                mapToHallDTO(screening.getHall())
        );
    }

    private MovieDTO mapToMovieDTO(Movie movie) {
        if (movie == null) return null;
        return new MovieDTO(movie.getId(), movie.getTitle());
    }

    private HallDTO mapToHallDTO(Hall hall) {
        if (hall == null) return null;
        return new HallDTO(hall.getId(), hall.getNumber(), hall.getRows(), hall.getSeatsPerRow());
    }

    public ScreeningDTO getScreeningById(Long id) {
        Screening screening = screeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening with ID " + id + " not found."));
        return mapToScreeningResponse(screening);    }

    public List<SeatDTO> getSeatsById(List<Long> ids) {
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
}