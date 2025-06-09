package com.example.reservation_service.kafka.consumer;

import com.example.reservation_service.entity.ScreeningInfo;
import com.example.reservation_service.entity.ScreeningSeatInfo;
import com.example.reservation_service.repository.ScreeningInfoRepository;
import com.example.reservation_service.repository.ScreeningSeatInfoRepository;
import com.example.reservation_service.service.ReservationService; // Do obsługi anulowania rezerwacji
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.HallDTO;
import org.example.commons.dto.ScreeningDTO;
import org.example.commons.dto.SeatDTO;
import org.example.commons.events.ScreeningCancelledEvent;
import org.example.commons.events.ScreeningCreatedEvent;
import org.example.commons.events.ScreeningUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ScreeningEventsConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(ScreeningEventsConsumer.class);
    private final ScreeningInfoRepository screeningCopyRepository;
    private final ScreeningSeatInfoRepository screeningSeatInfoRepository;
    private final ReservationService reservationService; // Do wywołania logiki anulowania rezerwacji

    @KafkaListener(topics = "cinema.screenings.created", // Użyj stałej z movie-service (lub zdefiniuj w commons)
            groupId = "cinema-group-reservation", // Lub dedykowana grupa
            containerFactory = "screeningCreatedEventKafkaListenerContainerFactory") // Dedykowana fabryka
    @Transactional
    public void handleScreeningCreated(ScreeningCreatedEvent event) {
        LOG.info("Received ScreeningCreatedEvent for screening ID: {}", event.getScreeningDTO().getId());
        ScreeningDTO dto = event.getScreeningDTO();
        ScreeningInfo copy = ScreeningInfo.builder()
                .id(dto.getId())
                .startTime(dto.getStartTime())
                .basePrice(dto.getBasePrice())
                .movieId(dto.getMovieDTO().getId())
                .movieTitle(dto.getMovieDTO().getTitle())
                .hallId(dto.getHallDTO().getId())
                .hallNumber(dto.getHallDTO().getNumber())
                .isActive(true)
                .build();
        ScreeningInfo savedScreeningCopy = screeningCopyRepository.save(copy);
        LOG.info("Screening copy created/updated for ID: {}", savedScreeningCopy.getId());

        // Generowanie i zapisywanie kopii miejsc dla tego seansu
        generateAndSaveSeatCopies(savedScreeningCopy, dto);
    }

    private void generateAndSaveSeatCopies(ScreeningInfo screeningInfo, ScreeningDTO screeningDtoFromEvent) {
        if (screeningDtoFromEvent.getAvailableSeats() == null || screeningDtoFromEvent.getAvailableSeats().isEmpty()) {
            LOG.warn("No seat DTOs provided in ScreeningCreatedEvent for screening copy ID {}. Cannot generate seat copies.", screeningInfo.getId());
            // Tutaj można by rzucić wyjątek lub wysłać event o błędzie, jeśli lista miejsc jest oczekiwana
            return;
        }

        List<ScreeningSeatInfo> seatCopies = new ArrayList<>();
        for (SeatDTO seatDto : screeningDtoFromEvent.getAvailableSeats()) {
            // Idempotentność dla miejsc: sprawdź, czy kopia miejsca już istnieje
            if (!screeningSeatInfoRepository.existsByScreeningInfoIdAndOriginalSeatId(screeningInfo.getId(), seatDto.getId())) {
                ScreeningSeatInfo seatCopy = ScreeningSeatInfo.builder()
                        .originalSeatId(seatDto.getId()) // ID miejsca z MovieService
                        .screeningInfoId(screeningInfo.getId()) // Poprawiona nazwa pola
                        .rowNumber(seatDto.getRowNumber())
                        .seatNumber(seatDto.getSeatNumber())
                        .build();
                seatCopies.add(seatCopy);
            } else {
                LOG.debug("ScreeningSeatInfo copy for screeningInfoId {} and originalSeatId {} already exists. Skipping creation.",
                        screeningInfo.getId(), seatDto.getId());
            }
        }

        if (!seatCopies.isEmpty()) {
            screeningSeatInfoRepository.saveAll(seatCopies);
            LOG.info("Saved {} new seat copies from event for screening copy ID: {}", seatCopies.size(), screeningInfo.getId());
        } else {
            LOG.info("No new seat copies to save for screening copy ID: {}", screeningInfo.getId());
        }
    }


    @KafkaListener(topics = "cinema.screenings.updated",
            groupId = "cinema-group-reservation",
            containerFactory = "screeningUpdatedEventKafkaListenerContainerFactory")
    @Transactional
    public void handleScreeningUpdated(ScreeningUpdatedEvent event) {
        LOG.info("Received ScreeningUpdatedEvent for screening ID: {}", event.getScreeningId());
        Optional<ScreeningInfo> oldScreeningCopyOpt = screeningCopyRepository.findById(event.getScreeningId());
        ScreeningInfo oldScreeningCopy = new ScreeningInfo(oldScreeningCopyOpt.get());

        screeningCopyRepository.findById(event.getScreeningId()).ifPresentOrElse(copy -> {
            ScreeningDTO dto = event.getUpdatedScreeningDTO();
            copy.setStartTime(dto.getStartTime());
            copy.setBasePrice(dto.getBasePrice());
            copy.setMovieId(dto.getMovieDTO().getId());
            copy.setMovieTitle(dto.getMovieDTO().getTitle());
            copy.setHallId(dto.getHallDTO().getId());
            copy.setHallNumber(dto.getHallDTO().getNumber());
            screeningCopyRepository.save(copy);
            reservationService.handleScreeningUpdate(event, oldScreeningCopy);
            LOG.info("Screening copy updated for ID: {}", copy.getId());
        }, () -> LOG.warn("Screening copy not found for update with ID: {}. A ScreeningCreatedEvent might have been missed.", event.getScreeningId()));
    }

    @KafkaListener(topics = "cinema.screenings.cancelled",
            groupId = "cinema-group-reservation",
            containerFactory = "screeningCancelledEventKafkaListenerContainerFactory")
    @Transactional
    public void handleScreeningCancelled(ScreeningCancelledEvent event) {
        LOG.info("Received ScreeningCancelledEvent for screening ID: {}", event.getScreeningId());
        screeningCopyRepository.findById(event.getScreeningId()).ifPresentOrElse(copy -> {
            copy.setActive(false);
            screeningCopyRepository.save(copy);
            LOG.info("Screening copy for ID: {} marked as inactive.", copy.getId());

            // Usuń kopie miejsc dla tego seansu (opcjonalnie, można je też oznaczyć jako nieaktywne)
            screeningSeatInfoRepository.deleteAllByScreeningInfoId(copy.getId());
            LOG.info("Deleted seat copies for inactive screening copy ID: {}", copy.getId());

            reservationService.handleScreeningCancellation(event.getScreeningId(), event.getReason());
        }, () -> LOG.warn("Screening copy not found for cancellation with ID: {}", event.getScreeningId()));
    }
}