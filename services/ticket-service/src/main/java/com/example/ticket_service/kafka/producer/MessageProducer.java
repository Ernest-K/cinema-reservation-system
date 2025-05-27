package com.example.ticket_service.kafka.producer;

import org.example.commons.dto.TicketDTO;
import org.example.commons.events.TicketGenerationFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);
    private static final String TOPIC = "cinema.notification";
    private static final String TICKET_FAILED_TOPIC = "cinema.ticket.failed";
    private final KafkaTemplate<String, TicketDTO> kafkaTemplate;
    private final KafkaTemplate<String, TicketGenerationFailedEvent> ticketFailedKafkaTemplate;

    public MessageProducer(
            KafkaTemplate<String, TicketDTO> ticketKafkaTemplate,
            KafkaTemplate<String, TicketGenerationFailedEvent> ticketFailedKafkaTemplate) {
        this.kafkaTemplate = ticketKafkaTemplate;
        this.ticketFailedKafkaTemplate = ticketFailedKafkaTemplate;
    }

    public void send(TicketDTO ticketDTO) {
        kafkaTemplate.send(TOPIC, ticketDTO);
    }

    public void sendTicketGenerationFailed(TicketGenerationFailedEvent event) {
        ticketFailedKafkaTemplate.send(TICKET_FAILED_TOPIC, event);
        LOG.info("Sent ticket generation failed event for reservation: {} to topic: {}", event.getReservationId(), TICKET_FAILED_TOPIC);
    }
}
