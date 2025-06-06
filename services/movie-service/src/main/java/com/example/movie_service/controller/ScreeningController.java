package com.example.movie_service.controller;

import com.example.movie_service.service.ScreeningService;
import jakarta.validation.Valid; // Dla walidacji DTO
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.ScreeningDTO; // Używamy DTO z commons
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated; // Dla walidacji parametrów ścieżki
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screenings") // Zmieniona ścieżka bazowa dla seansów
@RequiredArgsConstructor
@Validated // Włącz walidację parametrów w tej klasie
public class ScreeningController {

    private final ScreeningService screeningService;
    // MovieController ma już endpointy do pobierania seansów, tutaj skupimy się na CRUD

    @PostMapping
    public ResponseEntity<ScreeningDTO> createScreening(@Valid @RequestBody ScreeningDTO screeningDTO) {
        // ScreeningDTO przychodzące z requestu może nie mieć ID filmu i sali,
        // a zamiast tego ID obiektów Movie i Hall.
        // Lub ScreeningDTO może zawierać pełne MovieDTO i HallDTO.
        // Dla uproszczenia zakładam, że ScreeningDTO przekazuje ID dla Movie i Hall.
        // W serwisie będziemy musieli pobrać te encje.
        // Alternatywnie, stwórz CreateScreeningRequestDTO.
        ScreeningDTO createdScreening = screeningService.createScreening(screeningDTO);
        return new ResponseEntity<>(createdScreening, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreeningDTO> getScreeningById(
            @PathVariable("id") @NotNull @Positive(message = "Screening ID must be positive") Long id
    ) {
        ScreeningDTO screening = screeningService.getScreeningDetailsById(id); // Zmieniona nazwa metody w serwisie
        return ResponseEntity.ok(screening);
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ScreeningDTO>> getScreeningsByMovieId(
            @PathVariable("movieId") @NotNull @Positive(message = "Movie ID must be positive") Long movieId
    ) {
        List<ScreeningDTO> screenings = screeningService.getScreeningsByMovieId(movieId);
        return ResponseEntity.ok(screenings);
    }


    @PutMapping("/{id}")
    public ResponseEntity<ScreeningDTO> updateScreening(
            @PathVariable("id") @NotNull @Positive(message = "Screening ID must be positive") Long id,
            @Valid @RequestBody ScreeningDTO screeningDTO
    ) {
        // Upewnij się, że ID w ścieżce i w DTO (jeśli jest) są spójne, lub użyj ID ze ścieżki
        ScreeningDTO updatedScreening = screeningService.updateScreening(id, screeningDTO);
        return ResponseEntity.ok(updatedScreening);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelScreening(
            @PathVariable("id") @NotNull @Positive(message = "Screening ID must be positive") Long id
    ) {
        screeningService.cancelScreening(id, "");
        return ResponseEntity.noContent().build();
    }
}