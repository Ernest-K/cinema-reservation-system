package com.example.reservation_service.kafka.consumer;

import com.example.reservation_service.dto.ReservationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    @KafkaListener(topics = "cinema.reservation", groupId = "cinema-group")
    public void listen(ReservationDTO reservation) {
        LOG.info("Received reservation: {}", reservation);
    }
}
