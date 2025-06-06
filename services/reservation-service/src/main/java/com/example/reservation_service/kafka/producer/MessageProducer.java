package com.example.reservation_service.kafka.producer;

import org.example.commons.dto.ReservationDTO;
import org.example.commons.dto.ScreeningChangeNotificationDTO;
import org.example.commons.events.ReservationCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);
    private static final String RESERVATION_TOPIC = "cinema.reservation";
    private static final String TICKET_TOPIC = "cinema.ticket.request";
    private static final String CANCEL_TOPIC = "cinema.cancel.reservation";
    private static final String SCREENING_CHANGE_NOTIFICATION_TOPIC = "cinema.notification.screening_change";
    private final KafkaTemplate<String, ReservationDTO> kafkaTemplate;
    private final KafkaTemplate<String, ReservationCancelledEvent> cancelKafkaTemplate;
    private final KafkaTemplate<String, ScreeningChangeNotificationDTO> screeningChangeNotificationKafkaTemplate; // Nowy template

    public MessageProducer(KafkaTemplate<String, ReservationDTO> kafkaTemplate,
                           KafkaTemplate<String, ReservationCancelledEvent> cancelKafkaTemplate,
                           KafkaTemplate<String, ScreeningChangeNotificationDTO> screeningChangeNotificationKafkaTemplate) { // Zaktualizuj konstruktor
        this.kafkaTemplate = kafkaTemplate;
        this.cancelKafkaTemplate = cancelKafkaTemplate;
        this.screeningChangeNotificationKafkaTemplate = screeningChangeNotificationKafkaTemplate;
    }

    public void sendReservation(ReservationDTO reservation) {
        kafkaTemplate.send(RESERVATION_TOPIC, reservation);
        LOG.info("Sent reservation: {} to topic: {}", reservation.getId(), RESERVATION_TOPIC);
    }

    public void sendTicketRequest(ReservationDTO reservation) {
        kafkaTemplate.send(TICKET_TOPIC, reservation);
        LOG.info("Sent ticket request: {} to topic: {}", reservation.getId(), TICKET_TOPIC);
    }

    public void sendReservationCancelled(ReservationCancelledEvent event) {
        cancelKafkaTemplate.send(CANCEL_TOPIC, event);
        LOG.info("Sent cancellation: {} to topic: {}", event.getReservationId(), CANCEL_TOPIC);
    }

    public void sendScreeningChangeNotification(ScreeningChangeNotificationDTO payload) {
        LOG.info("Sending ScreeningChangeNotification for reservation ID: {} (Screening ID: {})",
                payload.getReservationId(), payload.getOriginalScreeningId());
        screeningChangeNotificationKafkaTemplate.send(SCREENING_CHANGE_NOTIFICATION_TOPIC, payload);
    }
}
