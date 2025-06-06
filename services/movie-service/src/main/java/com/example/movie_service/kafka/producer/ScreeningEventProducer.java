package com.example.movie_service.kafka.producer;

import lombok.RequiredArgsConstructor;
import org.example.commons.events.ScreeningCancelledEvent;
import org.example.commons.events.ScreeningCreatedEvent;
import org.example.commons.events.ScreeningUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScreeningEventProducer {
    private static final Logger LOG = LoggerFactory.getLogger(ScreeningEventProducer.class);

    public static final String SCREENING_CREATED_TOPIC = "cinema.screenings.created";
    public static final String SCREENING_UPDATED_TOPIC = "cinema.screenings.updated";
    public static final String SCREENING_CANCELLED_TOPIC = "cinema.screenings.cancelled";

    private final KafkaTemplate<String, ScreeningCreatedEvent> screeningCreatedKafkaTemplate;
    private final KafkaTemplate<String, ScreeningUpdatedEvent> screeningUpdatedKafkaTemplate;
    private final KafkaTemplate<String, ScreeningCancelledEvent> screeningCancelledKafkaTemplate;

    public void sendScreeningCreated(ScreeningCreatedEvent event) {
        LOG.info("Sending ScreeningCreatedEvent for screening ID: {}", event.getScreeningDTO().getId());
        screeningCreatedKafkaTemplate.send(SCREENING_CREATED_TOPIC, event);
    }

    public void sendScreeningUpdated(ScreeningUpdatedEvent event) {
        LOG.info("Sending ScreeningUpdatedEvent for screening ID: {}", event.getScreeningId());
        screeningUpdatedKafkaTemplate.send(SCREENING_UPDATED_TOPIC, event);
    }

    public void sendScreeningCancelled(ScreeningCancelledEvent event) {
        LOG.info("Sending ScreeningCancelledEvent for screening ID: {}", event.getScreeningId());
        screeningCancelledKafkaTemplate.send(SCREENING_CANCELLED_TOPIC, event);
    }
}
