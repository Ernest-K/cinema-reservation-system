package com.example.notification_service.kafka.consumer;

import com.example.notification_service.service.NotificationService;
import org.example.commons.dto.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);
    private final NotificationService notificationService;

    @KafkaListener(topics = "cinema.notification", groupId = "cinema-group")
    public void listen(TicketDTO ticketDTO) {
        LOG.info("Received ticket DTO: {}", ticketDTO);
        try {
            notificationService.sendTicketNotificationEmail(ticketDTO);
        } catch (Exception e) {
            LOG.error("Failed to process ticket notification request for customer {}: {}",
                    ticketDTO.getCustomerEmail(), e.getMessage(), e);
        }
    }
}
