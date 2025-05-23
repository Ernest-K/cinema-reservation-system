package com.example.ticket_service.kafka.producer;

import org.example.commons.dto.TicketDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);
    private static final String TOPIC = "cinema.notification";
    private final KafkaTemplate<String, TicketDTO> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, TicketDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(TicketDTO ticketDTO) {
        kafkaTemplate.send(TOPIC, ticketDTO);
    }
}
