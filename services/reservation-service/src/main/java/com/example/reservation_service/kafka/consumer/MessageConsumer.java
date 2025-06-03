package com.example.reservation_service.kafka.consumer;

import com.example.reservation_service.service.ReservationService;
import org.example.commons.dto.PaymentStatusDTO;
import lombok.RequiredArgsConstructor;
import org.example.commons.events.PaymentFailedEvent;
import org.example.commons.events.TicketGenerationFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final ReservationService reservationService;
    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    @KafkaListener(topics = "cinema.payment.status", groupId = "cinema-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(PaymentStatusDTO paymentStatusDTO) {
        LOG.info("Payment status received: {}", paymentStatusDTO);
        reservationService.updateReservationStatus(paymentStatusDTO);
    }

    @KafkaListener(topics = "cinema.ticket.failed", groupId = "cinema-group", containerFactory = "ticketFailedKafkaListenerContainerFactory")
    public void listenTicketGenerationFailed(TicketGenerationFailedEvent event) {
        LOG.info("Received ticket generation failed event for reservation: {}", event.getReservationId());
        try {
            reservationService.handleTicketGenerationFailure(event);
        } catch (Exception e) {
            LOG.error("Failed to process ticket generation failure for reservation {}: {}", event.getReservationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "cinema.failed.payment", groupId = "cinema-group", containerFactory = "paymentFailedKafkaListenerContainerFactory")
    public void listenPaymentGenerationFailed(PaymentFailedEvent event) {
        LOG.info("Received payment generation failed event for reservation: {}", event.getReservationId());
        try {
            reservationService.handlePaymentGenerationFailure(event);
        } catch (Exception e) {
            LOG.error("Failed to process payment generation failure for reservation {}: {}", event.getReservationId(), e.getMessage(), e);
        }
    }
}
