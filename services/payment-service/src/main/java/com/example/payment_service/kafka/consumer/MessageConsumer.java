package com.example.payment_service.kafka.consumer;

import com.example.payment_service.dto.ReservationDTO;
import com.example.payment_service.dto.TransactionRequest;
import com.example.payment_service.dto.TransactionResponse;
import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final PaymentService paymentService;
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
        pay.setGroupId(150); // Można przenieść do konfiguracji
        request.setPay(pay);

        TransactionResponse response = paymentService.createTransaction(request);

        System.out.println(response.toString());

//        PaymentTransaction transaction = new PaymentTransaction();
//        transaction.setTransactionId(response.getTransactionId());
//        transaction.setReservationId(reservation.getId());
//        transaction.setCreationDate(LocalDateTime.now());
//        transaction.setExpirationDate(LocalDateTime.now().plusMinutes(15));
//        transaction.setStatus("pending");
//        repository.save(transaction);
    }
}
