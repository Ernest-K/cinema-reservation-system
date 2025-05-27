package com.example.reservation_service.kafka.producer;

import org.example.commons.dto.ReservationDTO;
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
    private final KafkaTemplate<String, ReservationDTO> kafkaTemplate;
    private final KafkaTemplate<String, ReservationCancelledEvent> cancelKafkaTemplate;

    public MessageProducer(KafkaTemplate<String, ReservationDTO> kafkaTemplate, KafkaTemplate<String, ReservationCancelledEvent> cancelKafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.cancelKafkaTemplate = cancelKafkaTemplate;
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
}
