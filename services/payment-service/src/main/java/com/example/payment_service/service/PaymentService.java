package com.example.payment_service.service;

import com.example.payment_service.dto.TpayAuthResponse;
import com.example.payment_service.dto.TransactionRequest;
import com.example.payment_service.dto.TransactionResponse;
import com.example.payment_service.dto.TransactionStatusResponse;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.kafka.producer.MessageProducer;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.*;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
public class PaymentService {
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

    public PaymentService(MessageProducer messageProducer, RestTemplate restTemplate, PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
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
            System.out.println(response.getBody().getAccessToken());
            return response.getBody().getAccessToken();
        } else {
            throw new RuntimeException("Failed to obtain access token from Tpay");
        }
    }

    public TransactionResponse createTransaction(TransactionRequest request) {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                "https://api.tpay.com/transactions",
                entity,
                TransactionResponse.class
        );

        System.out.println(response.getBody());

        return response.getBody();
    }
//
//    public TransactionStatusResponse checkTransactionStatus(String transactionId) {
//        String token = getAccessToken();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(token);
//
//        HttpEntity<Void> entity = new HttpEntity<>(headers);
//        ResponseEntity<TransactionStatusResponse> response = restTemplate.exchange(
//                "https://api.tpay.com/transactions" + "/" + transactionId,
//                HttpMethod.GET,
//                entity,
//                TransactionStatusResponse.class
//        );
//
//        return response.getBody();
//    }
}
