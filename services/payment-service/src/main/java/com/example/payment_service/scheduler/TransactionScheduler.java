package com.example.payment_service.scheduler;

import com.example.payment_service.dto.PaymentStatusDTO;
import com.example.payment_service.dto.TransactionStatusResponse;
import com.example.payment_service.kafka.producer.MessageProducer;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
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
        paymentRepository.findByStatus("pending").forEach(payment -> {
            TransactionStatusResponse transactionStatusResponsea = paymentService.getTransactionStatus(payment.getTransactionId());

            System.out.println("scheduling");

            if ("correct".equals(transactionStatusResponsea.getStatus())) {
                payment.setStatus("completed");
                System.out.println("completed");
                PaymentStatusDTO paymentStatusDTO = new PaymentStatusDTO(payment.getId(), payment.getReservationId(), payment.getStatus());
                messageProducer.send(paymentStatusDTO);
            } else if (LocalDateTime.now().isAfter(payment.getExpirationDate())) {
                payment.setStatus("expired");
                System.out.println("expired");
                PaymentStatusDTO paymentStatusDTO = new PaymentStatusDTO(payment.getId(), payment.getReservationId(), payment.getStatus());
                messageProducer.send(paymentStatusDTO);
            }

            paymentRepository.save(payment);
        });
    }
}
