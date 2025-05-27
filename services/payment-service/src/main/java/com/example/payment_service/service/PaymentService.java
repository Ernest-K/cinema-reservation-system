package com.example.payment_service.service;

import com.example.payment_service.dto.TpayAuthResponse;
import com.example.payment_service.dto.TransactionRequest;
import com.example.payment_service.dto.TransactionResponse;
import com.example.payment_service.dto.TransactionStatusResponse;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.kafka.producer.MessageProducer;

import com.example.payment_service.repository.PaymentRepository;
import jakarta.ws.rs.NotFoundException;
import org.example.commons.dto.ReservationDTO;
import org.example.commons.events.PaymentFailedEvent;
import org.example.commons.events.ReservationCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.*;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private final RestTemplate restTemplate;
    private final PaymentRepository paymentRepository;
    private final MessageProducer messageProducer;

    @Value("${tpay.api.client-id}")
    private String clientId;

    @Value("${tpay.api.client-secret}")
    private String clientSecret;

    @Value("${tpay.api.auth.url}")
    private String authUrl;

    @Value("${tpay.api.transaction.url}")
    private String transactionUrl;

    @Value("${tpay.transaction.mock.enabled}")
    private boolean mockEnabled;

    public PaymentService(RestTemplate restTemplate, PaymentRepository paymentRepository, MessageProducer messageProducer) {
        this.restTemplate = restTemplate;
        this.paymentRepository = paymentRepository;
        this.messageProducer = messageProducer;
    }

    public String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<TpayAuthResponse> response = restTemplate.postForEntity(authUrl, request, TpayAuthResponse.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getAccessToken();
        } else {
            throw new RuntimeException("Failed to obtain access token from Tpay");
        }
    }

    public TransactionResponse createTransaction(ReservationDTO reservationDTO, TransactionRequest request) {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                    transactionUrl,
                    entity,
                    TransactionResponse.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println(response.getBody().getTransactionPaymentUrl());
                return response.getBody();
            }
        } catch (Exception e) {
            LOG.error("Error creating transaction: {}", e.getMessage(), e);
            messageProducer.sendPaymentFailed(new PaymentFailedEvent(reservationDTO.getId(), "Failed to create transaction: " + e.getMessage()));
        }

        return null;
    }

    public TransactionStatusResponse getTransactionStatus(String transactionId) {

        if (mockEnabled) {
            TransactionStatusResponse transactionStatusResponse = new TransactionStatusResponse();
            transactionStatusResponse.setStatus("correct");
            return transactionStatusResponse;
        }

        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<TransactionStatusResponse> response = restTemplate.exchange(
                transactionUrl + "/" + transactionId,
                HttpMethod.GET,
                entity,
                TransactionStatusResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to get transaction status");
        }
    }

    public void handleReservationCancellation(ReservationCancelledEvent event) {
        Payment payment = paymentRepository.findByReservationId(event.getReservationId())
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        if (payment == null) {
            LOG.info("No payment found for reservation {}. Skipping compensation.", event.getReservationId());
            return;
        }

        if ("cancelled".equals(payment.getStatus())) {
            LOG.info("Payment for reservation {} already cancelled. Skipping.", event.getReservationId());
            return;
        }

        LOG.info("Cancelling payment for reservation {}.", event.getReservationId());
        payment.setStatus("cancelled");
        paymentRepository.save(payment);
    }
}
