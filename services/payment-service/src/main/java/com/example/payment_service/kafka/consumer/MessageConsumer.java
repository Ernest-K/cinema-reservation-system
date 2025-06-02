package com.example.payment_service.kafka.consumer;

import org.example.commons.dto.ReservationDTO;
import com.example.payment_service.dto.TransactionRequest;
import com.example.payment_service.dto.TransactionResponse;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.example.commons.enums.PaymentStatus;
import org.example.commons.events.ReservationCancelledEvent;
import org.example.commons.exception.PaymentProcessingException;
import org.example.commons.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class MessageConsumer {
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    @KafkaListener(topics = "cinema.reservation", groupId = "cinema-group")
    public void listen(ReservationDTO reservation) {
        LOG.info("Received reservation creation event: Reservation ID {}", reservation.getId());

        if (paymentRepository.findByReservationId(reservation.getId()).isPresent()) {
            LOG.warn("Payment record already exists for reservation ID: {}. Skipping creation.", reservation.getId());
            return;
        }

        TransactionRequest request = new TransactionRequest();
        request.setAmount(reservation.getTotalAmount());
        request.setDescription("Payment for reservation: " + reservation.getId());

        TransactionRequest.Payer payer = new TransactionRequest.Payer();
        payer.setEmail(reservation.getCustomerEmail());
        payer.setName(reservation.getCustomerName());
        request.setPayer(payer);

        TransactionRequest.Pay pay = new TransactionRequest.Pay();
        pay.setGroupId(150);
        request.setPay(pay);

        try {
            TransactionResponse response = paymentService.createTransaction(reservation, request);

            Payment payment = new Payment();
            payment.setTransactionId(response.getTransactionId());
            payment.setReservationId(reservation.getId());
            payment.setCreationDate(LocalDateTime.now());
            payment.setPaymentUrl(response.getTransactionPaymentUrl());

            payment.setExpirationDate(LocalDateTime.now().plus(15, ChronoUnit.MINUTES));
            payment.setStatus(PaymentStatus.PENDING);

            paymentRepository.save(payment);
            LOG.info("Payment record created for reservation ID: {}. TPay Transaction ID: {}. Awaiting payment.",
                    reservation.getId(), payment.getTransactionId());

        } catch (PaymentProcessingException e) {
            LOG.error("PaymentProcessingException while creating transaction for reservation ID {}: {}", reservation.getId(), e.getMessage(), e);
        } catch (DataIntegrityViolationException e) {
            LOG.warn("Data integrity violation, likely duplicate payment processing for reservation ID {}: {}", reservation.getId(), e.getMessage());
        } catch (Exception e) {
            LOG.error("Unexpected error processing reservation event for ID {}: {}", reservation.getId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "cinema.cancel.reservation", groupId = "cinema-group", containerFactory = "cancelKafkaListenerContainerFactory")
    public void listenReservationCancelled(ReservationCancelledEvent event) {
        LOG.info("Received reservation cancellation event: Reservation ID {}", event.getReservationId());
        try {
            paymentService.handleReservationCancellation(event);
        } catch (ResourceNotFoundException e) {
            LOG.warn("Cannot process cancellation, payment not found for reservation ID {}: {}", event.getReservationId(), e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to process reservation cancellation for ID {}: {}", event.getReservationId(), e.getMessage(), e);
        }
    }
}
