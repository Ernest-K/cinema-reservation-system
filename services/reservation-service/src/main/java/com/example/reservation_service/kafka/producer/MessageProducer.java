package com.example.reservation_service.kafka.producer;

import com.example.reservation_service.dto.ReservationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);
    private static final String TOPIC = "cinema.reservation";
    private final KafkaTemplate<String, ReservationDTO> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, ReservationDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(ReservationDTO reservation) {
        kafkaTemplate.send(TOPIC, reservation);
        LOG.info("Sent reservation: {} to topic: {}", reservation.getId(), TOPIC);
    }
}
