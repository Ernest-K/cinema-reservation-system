package com.example.reservation_service.config;

import com.example.reservation_service.entity.ScreeningInfo;
import com.example.reservation_service.entity.ScreeningSeatInfo;
import com.example.reservation_service.repository.ScreeningInfoRepository;
import com.example.reservation_service.repository.ScreeningSeatInfoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(DataInitializer.class);

    private final ScreeningInfoRepository screeningInfoRepository;
    private final ScreeningSeatInfoRepository screeningSeatInfoRepository;

    // Domyślne wartości, jeśli ScreeningInfo nie dostarcza układu sali
    private static final int DEFAULT_HALL_ROWS = 10;
    private static final int DEFAULT_HALL_SEATS_PER_ROW = 15; // Zmniejszone dla mniej danych

    @PostConstruct
    @Transactional
    public void init() {
        if (screeningInfoRepository.count() > 0) {
            LOG.info("ScreeningInfo data already exists. Skipping initialization.");
            return;
        }
        LOG.info("Initializing ScreeningInfo and ScreeningSeatInfo data...");

        // Definicje sal z ich układami (ID sali, liczba rzędów, liczba miejsc w rzędzie)
        // Te ID sal (1L, 2L, 3L) powinny odpowiadać `hallId` używanym w `ScreeningInfo`
        Map<Long, HallLayout> hallLayouts = new HashMap<>();
        hallLayouts.put(1L, new HallLayout(10, 15)); // Sala 1: 10 rzędów, 12 miejsc/rząd
        hallLayouts.put(2L, new HallLayout(10, 15));  // Sala 2: 8 rzędów, 10 miejsc/rząd
        hallLayouts.put(3L, new HallLayout(10, 15)); // Sala 3: 12 rzędów, 15 miejsc/rząd

        List<ScreeningInfo> screeningInfoList = List.of(
                ScreeningInfo.builder()
                        .id(1L).startTime(LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES))
                        .basePrice(new BigDecimal("20.00")).movieId(1L).movieTitle("The Shawshank Redemption")
                        .hallId(1L).hallNumber(1).isActive(true) // Używa hallId = 1L
                        // Poniższe pola hallRows/hallSeatsPerRow są teraz redundantne, jeśli czerpiemy z hallLayouts
                        // .hallRows(hallLayouts.get(1L).rows).hallSeatsPerRow(hallLayouts.get(1L).seatsPerRow)
                        .build(),
                ScreeningInfo.builder()
                        .id(2L).startTime(LocalDateTime.now().plusHours(2).truncatedTo(ChronoUnit.MINUTES))
                        .basePrice(new BigDecimal("25.00")).movieId(2L).movieTitle("The Godfather")
                        .hallId(2L).hallNumber(2).isActive(true) // Używa hallId = 2L
                        // .hallRows(hallLayouts.get(2L).rows).hallSeatsPerRow(hallLayouts.get(2L).seatsPerRow)
                        .build(),
                ScreeningInfo.builder()
                        .id(3L).startTime(LocalDateTime.now().plusHours(5).truncatedTo(ChronoUnit.MINUTES))
                        .basePrice(new BigDecimal("20.00")).movieId(3L).movieTitle("Inception")
                        .hallId(3L).hallNumber(3).isActive(true) // Używa hallId = 3L
                        // .hallRows(hallLayouts.get(3L).rows).hallSeatsPerRow(hallLayouts.get(3L).seatsPerRow)
                        .build()
        );
        // Dodanie pól hallRows i hallSeatsPerRow do ScreeningInfo na podstawie mapy hallLayouts
        screeningInfoList.forEach(si -> {
            HallLayout layout = hallLayouts.get(si.getHallId());
            if (layout != null) {
                si.setHallRows(layout.rows);
                si.setHallSeatsPerRow(layout.seatsPerRow);
            } else {
                // Użyj domyślnych, jeśli ID sali nie ma w mapie (co nie powinno się zdarzyć przy tej logice)
                si.setHallRows(DEFAULT_HALL_ROWS);
                si.setHallSeatsPerRow(DEFAULT_HALL_SEATS_PER_ROW);
                LOG.warn("No layout defined for hallId: {}. Using default layout for screeningId: {}", si.getHallId(), si.getId());
            }
        });


        screeningInfoRepository.saveAll(screeningInfoList);
        LOG.info("Saved {} ScreeningInfo records.", screeningInfoList.size());

        // Mapa do generowania unikalnych "originalSeatId" per sala
        // Klucz: hallId, Wartość: następne dostępne ID miejsca dla tej sali
        Map<Long, Long> nextSeatIdPerHall = new HashMap<>();

        for (ScreeningInfo screeningInfo : screeningInfoList) {
            HallLayout layout = hallLayouts.get(screeningInfo.getHallId());
            int rows = (layout != null) ? layout.rows : DEFAULT_HALL_ROWS;
            int seatsPerRow = (layout != null) ? layout.seatsPerRow : DEFAULT_HALL_SEATS_PER_ROW;

            // Pobierz lub zainicjuj licznik ID dla danej sali
            // Zaczynamy ID miejsc dla danej sali od (hallId * 1000) + 1
            // np. sala 1: 1001, 1002... sala 2: 2001, 2002...
            long currentSeatIdCounter = nextSeatIdPerHall.computeIfAbsent(screeningInfo.getHallId(), k -> k);

            List<ScreeningSeatInfo> seatInfos = new ArrayList<>();
            // Sprawdź, czy miejsca dla tej sali (tego konkretnego screeningInfo.hallId) nie zostały już wygenerowane
            // przez inny seans w tej samej sali w ramach tej sesji DataInitializer.
            // To bardziej skomplikowane, jeśli wiele ScreeningInfo współdzieli ten sam hallId.
            // Lepiej generować miejsca raz per UNIKALNE HallId.
            // Jednak ScreeningSeatInfo jest PER ScreeningInfo, więc musimy je tworzyć dla każdego.
            // Problem polega na tym, jak zapewnić spójne `originalSeatId` dla tych samych miejsc w tej samej sali
            // dla różnych seansów.

            // Uproszczenie: Załóżmy, że MovieService w evencie ScreeningCreatedEvent dostarczyłby
            // listę SeatDTO z ich *prawdziwymi, globalnie unikalnymi ID* (z encji Seat w MovieService).
            // Tutaj symulujemy, że te ID są generowane.
            // Dla spójności, jeśli dwa ScreeningInfo używają tej samej sali (np. hallId=1),
            // powinny mieć te same `originalSeatId` dla odpowiadających im miejsc.
            // Poniższa logika z `nextSeatIdPerHall` nie zapewni tego idealnie, jeśli sale są współdzielone.

            // Poprawiona logika generowania originalSeatId: ID miejsca jest unikalne W OBRĘBIE SALI.
            // Tworzymy je jako kombinację hallId, rzędu i numeru miejsca dla symulacji.
            // W realnym systemie, to `MovieService` jest właścicielem tych ID.
            int seatId = 1;

            for (int r = 1; r <= rows; r++) {
                for (int s = 1; s <= seatsPerRow; s++) {
                    // Symulowane originalSeatId: unikalne dla kombinacji (hallId, rząd, numer_miejsca)
                    // To jest tylko placeholder. Prawdziwe ID przyszłyby z MovieService.
                    long simulatedOriginalSeatId = seatId;

                    seatId++;

                    ScreeningSeatInfo seatInfo = ScreeningSeatInfo.builder()
                            .originalSeatId(simulatedOriginalSeatId)
                            .screeningInfoId(screeningInfo.getId()) // Powiązanie z konkretną kopią seansu
                            .rowNumber(r)
                            .seatNumber(s)
                            .build();
                    seatInfos.add(seatInfo);
                }
            }
            screeningSeatInfoRepository.saveAll(seatInfos);
            LOG.debug("Saved {} seat copies for screeningInfoId: {}", seatInfos.size(), screeningInfo.getId());
        }
        LOG.info("Finished initializing ScreeningInfo and ScreeningSeatInfo data.");
    }

    // Pomocnicza klasa do przechowywania układu sali
    private static class HallLayout {
        int rows;
        int seatsPerRow;

        HallLayout(int rows, int seatsPerRow) {
            this.rows = rows;
            this.seatsPerRow = seatsPerRow;
        }
    }
}