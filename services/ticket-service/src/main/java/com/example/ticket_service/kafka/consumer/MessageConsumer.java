package com.example.ticket_service.kafka.consumer;

import com.example.ticket_service.dto.ReservationDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    @KafkaListener(topics = "cinema.ticket.request", groupId = "cinema-group")
    public void listen(ReservationDTO reservationDTO) {
        LOG.info("Received ticket generation request for reservation: {}", reservationDTO);
        // TODO: Add schema/dto service
        // generateTicket(reservationDTO);
    }
}
