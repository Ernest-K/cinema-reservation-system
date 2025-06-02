package com.example.payment_service.scheduler;

import org.example.commons.dto.PaymentStatusDTO;
import com.example.payment_service.dto.TransactionStatusResponse;
import com.example.payment_service.kafka.producer.MessageProducer;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.example.commons.enums.PaymentStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@EnableScheduling
@Component
@RequiredArgsConstructor
public class TransactionScheduler {
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final MessageProducer messageProducer;

    @Scheduled(fixedRate = 5000)
    public void checkTransactions() {
        paymentRepository.findByStatus(PaymentStatus.PENDING).forEach(payment -> {
            TransactionStatusResponse transactionStatusResponse = paymentService.getTransactionStatus(payment.getTransactionId());

            System.out.println("scheduling");

            if ("correct".equals(transactionStatusResponse.getStatus())) {
                payment.setStatus(PaymentStatus.COMPLETED);
                System.out.println("completed");
                PaymentStatusDTO paymentStatusDTO = new PaymentStatusDTO(payment.getId(), payment.getReservationId(), payment.getStatus());
                messageProducer.send(paymentStatusDTO);
            } else if (LocalDateTime.now().isAfter(payment.getExpirationDate())) {
                payment.setStatus(PaymentStatus.EXPIRED);
                System.out.println("expired");
                PaymentStatusDTO paymentStatusDTO = new PaymentStatusDTO(payment.getId(), payment.getReservationId(), payment.getStatus());
                messageProducer.send(paymentStatusDTO);
            }

            paymentRepository.save(payment);
        });
    }
}
