package com.example.reservation_service.kafka.consumer;

import com.example.reservation_service.entity.ScreeningInfo;
import com.example.reservation_service.repository.ScreeningInfoRepository;
import com.example.reservation_service.service.ReservationService; // Do obsługi anulowania rezerwacji
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.ScreeningDTO;
import org.example.commons.events.ScreeningCancelledEvent;
import org.example.commons.events.ScreeningCreatedEvent;
import org.example.commons.events.ScreeningUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ScreeningEventsConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(ScreeningEventsConsumer.class);
    private final ScreeningInfoRepository screeningCopyRepository;
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
        screeningCopyRepository.save(copy);
        LOG.info("Screening copy created/updated for ID: {}", copy.getId());
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
            screeningCopyRepository.save(copy);
            LOG.info("Screening copy updated for ID: {}", copy.getId());
            reservationService.handleScreeningUpdate(event, oldScreeningCopy);

        }, () -> LOG.warn("Screening copy not found for update with ID: {}. A ScreeningCreatedEvent might have been missed.", event.getScreeningId()));
    }

    @KafkaListener(topics = "cinema.screenings.cancelled",
            groupId = "cinema-group-reservation",
            containerFactory = "screeningCancelledEventKafkaListenerContainerFactory")
    @Transactional
    public void handleScreeningCancelled(ScreeningCancelledEvent event) {
        LOG.info("Received ScreeningCancelledEvent for screening ID: {}", event.getScreeningId());
        screeningCopyRepository.findById(event.getScreeningId()).ifPresentOrElse(copy -> {
            copy.setActive(false); // Oznacz seans jako nieaktywny
            screeningCopyRepository.save(copy);
            LOG.info("Screening copy for ID: {} marked as inactive.", copy.getId());

            // Logika kompensacyjna: Anuluj wszystkie rezerwacje na ten seans
            // i powiadom użytkowników
            reservationService.handleScreeningCancellation(event.getScreeningId(), event.getReason());
        }, () -> LOG.warn("Screening copy not found for cancellation with ID: {}", event.getScreeningId()));
    }
}