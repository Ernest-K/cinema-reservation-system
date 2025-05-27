package com.example.payment_service.kafka.consumer;

import org.example.commons.dto.ReservationDTO;
import com.example.payment_service.dto.TransactionRequest;
import com.example.payment_service.dto.TransactionResponse;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.example.commons.events.ReservationCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MessageConsumer {
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    @KafkaListener(topics = "cinema.reservation", groupId = "cinema-group")
    public void listen(ReservationDTO reservation) {
        LOG.info("Received reservation: {}", reservation);
        TransactionRequest request = new TransactionRequest();
        request.setAmount(reservation.getTotalAmount());
        request.setDescription("Płatność za rezerwację: " + reservation.getId());
        TransactionRequest.Payer payer = new TransactionRequest.Payer();
        payer.setEmail(reservation.getCustomerEmail());
        payer.setName(reservation.getCustomerName());
        request.setPayer(payer);
        TransactionRequest.Pay pay = new TransactionRequest.Pay();
        pay.setGroupId(150);
        request.setPay(pay);

        TransactionResponse response = paymentService.createTransaction(reservation, request);
        if (response == null) {
            LOG.error("Failed to create transaction for reservation: {}", reservation.getId());
            return;
        }

        Payment payment = new Payment();
        payment.setTransactionId(response.getTransactionId());
        payment.setReservationId(reservation.getId());
        payment.setCreationDate(LocalDateTime.now());
        payment.setExpirationDate(LocalDateTime.now().plusMinutes(1));
        payment.setStatus("pending");

        paymentRepository.save(payment);
    }

    @KafkaListener(topics = "cinema.cancel.reservation", groupId = "cinema-group", containerFactory = "cancelKafkaListenerContainerFactory")
    public void listenReservationCancelled(ReservationCancelledEvent event) {
        LOG.info("Received reservation cancellation: {}", event.getReservationId());
        try {
            paymentService.handleReservationCancellation(event);
        } catch (Exception e) {
            LOG.error("Failed to process reservation cancellation for ID {}: {}", event.getReservationId(), e.getMessage(), e);
        }
    }
}
