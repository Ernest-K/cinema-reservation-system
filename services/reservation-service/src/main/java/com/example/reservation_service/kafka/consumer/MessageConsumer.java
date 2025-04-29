package com.example.reservation_service.kafka.consumer;

import com.example.reservation_service.dto.PaymentStatusDTO;
import com.example.reservation_service.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final ReservationService reservationService;
    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    @KafkaListener(topics = "cinema.payment.status", groupId = "cinema-group")
    public void listen(PaymentStatusDTO paymentStatusDTO) {
        LOG.info("Payment status received: {}", paymentStatusDTO);
        reservationService.updateReservationStatus(paymentStatusDTO);
    }
}
