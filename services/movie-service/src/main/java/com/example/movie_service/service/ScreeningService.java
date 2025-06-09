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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private static final Logger LOG = LoggerFactory.getLogger(ScreeningService.class);

    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;
    private final HallRepository hallRepository; // Poprawiona nazwa repozytorium
    // SeatRepository nie jest potrzebne, jeśli Hall przechowuje swoje miejsca
    private final ScreeningEventProducer screeningEventProducer;

    @Transactional
    public ScreeningDTO createScreening(ScreeningDTO screeningRequestDTO) {
        // ... (walidacja jak poprzednio) ...
        if (screeningRequestDTO.getMovieDTO() == null || screeningRequestDTO.getMovieDTO().getId() == null) {
            throw new IllegalArgumentException("Movie ID must be provided for creating a screening.");
        }
        if (screeningRequestDTO.getHallDTO() == null || screeningRequestDTO.getHallDTO().getId() == null) {
            throw new IllegalArgumentException("Hall ID must be provided for creating a screening.");
        }

        Movie movie = movieRepository.findById(screeningRequestDTO.getMovieDTO().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with ID: " + screeningRequestDTO.getMovieDTO().getId()));

        // Pobieramy Hall wraz z jego miejscami (zakładając, że relacja jest EAGER lub używamy @EntityGraph)
        // Lub jeśli HallRepository ma metodę findByIdWithSeats
        Hall hall = hallRepository.findById(screeningRequestDTO.getHallDTO().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with ID: " + screeningRequestDTO.getHallDTO().getId()));

        Screening newScreening = Screening.builder()
                .startTime(screeningRequestDTO.getStartTime())
                .basePrice(screeningRequestDTO.getBasePrice())
                .movie(movie)
                .hall(hall) // Hall jest teraz przypisany
                .build();

        Screening savedScreening = screeningRepository.save(newScreening);
        LOG.info("Screening created successfully with ID: {}", savedScreening.getId());

        // Teraz mapToScreeningDTO musi poprawnie wypełnić listę miejsc
        ScreeningDTO responseDTO = mapToScreeningDTOWithSeats(savedScreening);
        screeningEventProducer.sendScreeningCreated(new ScreeningCreatedEvent(responseDTO));

        return responseDTO;
    }

    // Zmiana nazwy metody dla jasności
    private ScreeningDTO mapToScreeningDTOWithSeats(Screening screening) {
        MovieDTO movieDTO = mapToMovieDTO(screening.getMovie());
        HallDTO hallDTO = mapToHallDTO(screening.getHall()); // Ta metoda powinna mapować też informacje o układzie sali

        // Pobierz miejsca bezpośrednio z encji Hall powiązanej z seansem
        List<SeatDTO> seatDTOs;
        if (screening.getHall() != null && screening.getHall().getSeats() != null) {
            seatDTOs = screening.getHall().getSeats().stream()
                    .map(this::mapToSeatDTO) // Metoda mapująca Seat na SeatDTO
                    .collect(Collectors.toList());
        } else {
            LOG.warn("Hall or seats for screening ID {} are null. Seat list will be empty in DTO.", screening.getId());
            seatDTOs = Collections.emptyList();
        }

        return new ScreeningDTO(
                screening.getId(),
                screening.getStartTime(),
                screening.getBasePrice(),
                movieDTO,
                hallDTO,
                seatDTOs // Przekazujemy listę SeatDTO
        );
    }

    // Metoda do mapowania pojedynczej encji Seat na SeatDTO
    private SeatDTO mapToSeatDTO(Seat seat) {
        if (seat == null) return null;
        return new SeatDTO(seat.getId(), seat.getRowNumber(), seat.getSeatNumber());
    }


    @Transactional(readOnly = true)
    public ScreeningDTO getScreeningDetailsById(Long id) {
        LOG.debug("Fetching screening details for ID: {}", id);
        Screening screening = screeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening not found with ID: " + id));
        return mapToScreeningDTOWithSeats(screening); // Użyj metody, która mapuje też miejsca
    }

    @Transactional(readOnly = true)
    public List<ScreeningDTO> getScreeningsByMovieId(Long movieId) {
        // ... (jak poprzednio, ale użyj mapToScreeningDTOWithSeats)
        LOG.debug("Fetching screenings for movie ID: {}", movieId);
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie not found with ID: " + movieId + ", cannot fetch screenings.");
        }
        List<Screening> screenings = screeningRepository.findAllByMovieId(movieId);
        return screenings.stream().map(this::mapToScreeningDTOWithSeats).collect(Collectors.toList());
    }


    @Transactional
    public ScreeningDTO updateScreening(Long screeningId, ScreeningDTO screeningUpdateDTO) {
        // ... (logika aktualizacji jak poprzednio) ...
        Screening existingScreening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening not found with ID: " + screeningId + " for update."));

        // ... (aktualizacja pól startTime, basePrice, movie, hall) ...
        if (screeningUpdateDTO.getStartTime() != null) {
            existingScreening.setStartTime(screeningUpdateDTO.getStartTime());
        }
        if (screeningUpdateDTO.getBasePrice() != null) {
            existingScreening.setBasePrice(screeningUpdateDTO.getBasePrice());
        }
        // ... obsługa zmiany filmu i sali ...

        Screening updatedScreening = screeningRepository.save(existingScreening);
        LOG.info("Screening with ID: {} updated successfully.", updatedScreening.getId());

        // Użyj mapowania z miejscami dla eventu
        ScreeningDTO responseDTO = mapToScreeningDTOWithSeats(updatedScreening);
        screeningEventProducer.sendScreeningUpdated(new ScreeningUpdatedEvent(updatedScreening.getId(), responseDTO));

        return responseDTO;
    }

    // Metoda cancelScreening pozostaje bez zmian w kontekście mapowania,
    // wysyła tylko ID anulowanego seansu.
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

    // Oryginalna metoda mapToScreeningDTO (może być teraz prywatna lub usunięta, jeśli mapToScreeningDTOWithSeats ją zastępuje)
    // Ta metoda nie zawierała listy miejsc, co było problemem
    private ScreeningDTO mapToScreeningDTONoSeats(Screening screening) { // Zmieniona nazwa dla odróżnienia
        Hall hall = screening.getHall();
        HallDTO hallDTO = new HallDTO(
                hall.getId(),
                hall.getNumber(),
                hall.getRows(),
                hall.getSeatsPerRow()
        );
        return new ScreeningDTO(
                screening.getId(),
                screening.getStartTime(),
                screening.getBasePrice(),
                mapToMovieDTO(screening.getMovie()),
                hallDTO,
                null // Brak miejsc
        );
    }

    private MovieDTO mapToMovieDTO(Movie movie) {
        if (movie == null) return null;
        return new MovieDTO(movie.getId(), movie.getTitle());
    }

    private HallDTO mapToHallDTO(Hall hall) {
        if (hall == null) return null;
        // Upewnij się, że HallDTO ma pola rows i seatsPerRow (w commons/dto/HallDTO.java)
        // public class HallDTO { private Long id; private int number; private int rows; private int seatsPerRow; }
        return new HallDTO(hall.getId(), hall.getNumber(), hall.getRows(), hall.getSeatsPerRow());
    }

    // Metody getScreeningById i getSeatsById (te publiczne z kontrolera) wydają się być zduplikowane
    // z getScreeningDetailsById. Rozważ usunięcie lub refaktoryzację.
    // Poniżej zostawiam je, ale wskazuję, że `getScreeningById` powinno używać `mapToScreeningDTOWithSeats`.

    public ScreeningDTO getScreeningById(Long id) { // Ta metoda powinna być prawdopodobnie `getScreeningDetailsById`
        Screening screening = screeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening with ID " + id + " not found."));
        return mapToScreeningDTOWithSeats(screening); // Użyj mapowania z miejscami
    }

    // Metoda getSeatsById była do pobierania konkretnych SeatDTO po ich ID,
    // co jest potrzebne dla ReservationService, jeśli nie replikuje on miejsc.
    // Jeśli ReservationService replikuje miejsca, ta metoda może nie być już wywoływana przez niego.
    // Ale może być używana przez inne części systemu lub do celów administracyjnych.
    @Transactional(readOnly = true)
    public List<SeatDTO> getSeatsByIds(List<Long> ids) {
        LOG.debug("Fetching seats by IDs: {}", ids);
        if (ids == null || ids.isEmpty()) {
            LOG.warn("Attempted to fetch seats with a null or empty ID list.");
            return Collections.emptyList();
        }

        // Użyj SeatRepository do pobrania encji Seat
        List<Seat> seats = seatRepository.findAllById(ids);

        // Opcjonalne: Sprawdzenie, czy wszystkie żądane ID zostały znalezione
        if (seats.size() < ids.size()) {
            List<Long> foundIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
            List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toList());
            LOG.warn("Could not find all requested seats. Requested IDs: {}, Found IDs: {}, Missing IDs: {}", ids, foundIds, missingIds);
            // Możesz zdecydować, czy rzucić tutaj ResourceNotFoundException,
            // czy po prostu zwrócić te, które zostały znalezione.
            // Obecnie zwracamy tylko znalezione.
        }

        if (seats.isEmpty() && !ids.isEmpty()) {
            LOG.warn("No seats found for the provided IDs: {}", ids);
            // Można rzucić ResourceNotFoundException, jeśli oczekujesz, że przynajmniej jedno ID powinno pasować
            // throw new ResourceNotFoundException("No seats found for the provided IDs: " + ids);
        }

        // Zmapuj znalezione encje Seat na SeatDTO
        return seats.stream()
                .map(this::mapToSeatDTO) // Użyj istniejącej metody mapującej
                .collect(Collectors.toList());
    }

    // Ta metoda była używana w Twoim poprzednim kodzie ReservationService.
    // Jeśli ReservationService teraz polega na ScreeningSeatCopy, to ta metoda
    // może nie być już potrzebna dla niego, ale zostawiam ją.
    private ScreeningDTO mapToScreeningResponse(Screening screening) { // To jest prawdopodobnie to samo co mapToScreeningDTONoSeats
        return new ScreeningDTO(screening.getId(), screening.getStartTime(), screening.getBasePrice(), mapToMovieDTO(screening.getMovie()), mapToHallDTO(screening.getHall()), null);
    }

    // Ta metoda była używana w Twoim poprzednim kodzie ReservationService.
    private SeatDTO mapToSeatResponse(Seat seat) { // To jest prawdopodobnie to samo co mapToSeatDTO
        return new SeatDTO(seat.getId(), seat.getRowNumber(), seat.getSeatNumber());
    }
}