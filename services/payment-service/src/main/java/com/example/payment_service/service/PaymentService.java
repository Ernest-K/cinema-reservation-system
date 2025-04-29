package com.example.payment_service.service;

import com.example.payment_service.dto.TpayAuthResponse;
import com.example.payment_service.dto.TransactionRequest;
import com.example.payment_service.dto.TransactionResponse;
import com.example.payment_service.dto.TransactionStatusResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.*;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class PaymentService {
    private final RestTemplate restTemplate;

    @Value("${tpay.api.client-id}")
    private String clientId;

    @Value("${tpay.api.client-secret}")
    private String clientSecret;

    @Value("${tpay.api.auth.url}")
    private String authUrl;

    @Value("${tpay.api.transaction.url}")
    private String transactionUrl;

    public PaymentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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

    public TransactionResponse createTransaction(TransactionRequest request) {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                transactionUrl,
                entity,
                TransactionResponse.class
        );


        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            System.out.println(response.getBody().getTransactionPaymentUrl());
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to create transaction");
        }
    }

    public TransactionStatusResponse getTransactionStatus(String transactionId) {
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
}
