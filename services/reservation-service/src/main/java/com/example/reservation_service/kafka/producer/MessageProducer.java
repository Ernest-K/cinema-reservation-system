package com.example.reservation_service.kafka.producer;

import com.example.reservation_service.dto.ReservationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);
    private static final String RESERVATION_TOPIC = "cinema.reservation";
    private static final String TICKET_TOPIC = "cinema.ticket.request";
    private final KafkaTemplate<String, ReservationDTO> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, ReservationDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendReservation(ReservationDTO reservation) {
        kafkaTemplate.send(RESERVATION_TOPIC, reservation);
        LOG.info("Sent reservation: {} to topic: {}", reservation.getId(), RESERVATION_TOPIC);
    }

    public void sendTicketRequest(ReservationDTO reservation) {
        kafkaTemplate.send(TICKET_TOPIC, reservation);
        LOG.info("Sent ticket request: {} to topic: {}", reservation.getId(), TICKET_TOPIC);
    }
}
