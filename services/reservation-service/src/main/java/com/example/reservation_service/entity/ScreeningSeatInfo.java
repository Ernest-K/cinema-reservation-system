package com.example.reservation_service.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "screening_seat_info")
public class ScreeningSeatInfo {

    // Używamy ID oryginalnego miejsca z MovieService jako ID tej kopii.
    // To zakłada, że ID miejsc są unikalne globalnie lub przynajmniej w kontekście MovieService.
    // Jeśli ID miejsc są generowane per sala, to musimy użyć złożonego klucza (screeningCopyId, seatIdFromMovieService)
    // lub własnego generowanego ID dla tej kopii.
    // Dla uproszczenia, załóżmy, że Seat.id z MovieService jest wystarczająco unikalne
    // lub że przekazujemy je i używamy jako referencji.
    // Bezpieczniej: własne ID dla tej encji.
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    // private Long id;

    // Alternatywa: użyj oryginalnego ID miejsca jako część klucza lub jako zwykłe pole.
    // Jeśli ma być częścią klucza, potrzebujesz @EmbeddedId lub @IdClass.
    // Na razie jako zwykłe pole, a @Id będzie generowane.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Własne ID tej kopii

    @Column(nullable = false)
    private Long originalSeatId; // ID miejsca z MovieService

    @Column(nullable = false)
    private Long screeningInfoId; // Powiązanie z ScreeningCopy

    @Column(nullable = false)
    private int rowNumber;

    @Column(nullable = false)
    private int seatNumber;

    // Nie potrzebujemy tu flagi 'isAvailable' bezpośrednio, bo dostępność
    // będzie wynikać z tego, czy istnieje ReservedSeat dla tego miejsca na ten seans.
    // Flaga 'isAvailable' w SeatAvailabilityDTO była dynamicznie obliczana.
}
