package com.example.payment_service.service;

import com.example.payment_service.dto.TpayAuthResponse;
import com.example.payment_service.dto.TransactionRequest;
import com.example.payment_service.dto.TransactionResponse;
import com.example.payment_service.dto.TransactionStatusResponse;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.kafka.producer.MessageProducer;

import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.ReservationDTO;
import org.example.commons.enums.PaymentStatus;
import org.example.commons.events.PaymentFailedEvent;
import org.example.commons.events.ReservationCancelledEvent;
import org.example.commons.exception.PaymentProcessingException;
import org.example.commons.exception.TpayCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.*;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
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

    public String getAccessToken() {
        LOG.debug("Requesting TPay access token.");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TpayAuthResponse> response = restTemplate.postForEntity(authUrl, requestEntity, TpayAuthResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getAccessToken() != null) {
                LOG.info("Successfully obtained TPay access token.");
                return response.getBody().getAccessToken();
            } else {
                LOG.error("Failed to obtain access token from TPay. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                throw new TpayCommunicationException("Failed to obtain access token from TPay. Status: " + response.getStatusCode(), response.getStatusCodeValue());
            }
        } catch (HttpStatusCodeException e) {
            LOG.error("HTTP error obtaining TPay access token. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new TpayCommunicationException("HTTP error obtaining TPay access token: " + e.getStatusCode(), e, e.getStatusCode().value());
        } catch (RestClientException e) {
            LOG.error("Error connecting to TPay for access token: {}", e.getMessage(), e);
            throw new TpayCommunicationException("Communication error while obtaining TPay access token.", e);
        }
    }

    public TransactionResponse createTransaction(ReservationDTO reservationDTO, TransactionRequest request) {
        LOG.info("Creating TPay transaction for reservation ID: {}", reservationDTO.getId());
        String token;
        try {
            token = getAccessToken();
        } catch (TpayCommunicationException e) {
            LOG.error("Failed to create transaction due to auth error for reservation ID {}: {}", reservationDTO.getId(), e.getMessage());
            messageProducer.sendPaymentFailed(new PaymentFailedEvent(reservationDTO.getId(), "TPay authentication failed: " + e.getMessage()));
            throw new PaymentProcessingException("Failed to create transaction - TPay auth error.", e);
        }

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
            if ((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode().is2xxSuccessful()) && response.getBody() != null) {
                LOG.info("TPay transaction created successfully for reservation ID: {}. TPay Transaction ID: {}, Payment URL: {}",
                        reservationDTO.getId(), response.getBody().getTransactionId(), response.getBody().getTransactionPaymentUrl());

                System.out.println(response.getBody().getTransactionPaymentUrl());

                return response.getBody();
            } else {
                LOG.error("Failed to create TPay transaction for reservation ID: {}. Status: {}, Body: {}",
                        reservationDTO.getId(), response.getStatusCode(), response.getBody());
                String errorDetail = "TPay API returned status " + response.getStatusCode();
                messageProducer.sendPaymentFailed(new PaymentFailedEvent(reservationDTO.getId(), errorDetail));
                throw new PaymentProcessingException("Failed to create TPay transaction. Status: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            LOG.error("HTTP error creating TPay transaction for reservation ID: {}. Status: {}, Response: {}",
                    reservationDTO.getId(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            messageProducer.sendPaymentFailed(new PaymentFailedEvent(reservationDTO.getId(), "HTTP error during TPay transaction creation: " + e.getStatusCode()));
            throw new PaymentProcessingException("HTTP error during TPay transaction creation: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            LOG.error("Error connecting to TPay for transaction creation (Reservation ID: {}): {}", reservationDTO.getId(), e.getMessage(), e);
            messageProducer.sendPaymentFailed(new PaymentFailedEvent(reservationDTO.getId(), "Communication error during TPay transaction creation."));
            throw new PaymentProcessingException("Communication error during TPay transaction creation.", e);
        }
    }

    public TransactionStatusResponse getTransactionStatus(String transactionId) {
        LOG.debug("Fetching TPay transaction status for TPay ID: {}", transactionId);
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("TPay Transaction ID cannot be null or empty.");
        }

        if (mockEnabled) {
            LOG.info("TPay mock enabled. Returning 'correct' status for TPay ID: {}", transactionId);
            TransactionStatusResponse mockResponse = new TransactionStatusResponse();
            mockResponse.setStatus("correct"); // Symulacja poprawnej płatności
            mockResponse.setTransactionId(transactionId);
            return mockResponse;
        }

        String token;
        try {
            token = getAccessToken();
        } catch (TpayCommunicationException e) {
            LOG.error("Failed to get transaction status for TPay ID {} due to auth error: {}", transactionId, e.getMessage());
            // W tym kontekście rzucenie wyjątku jest lepsze niż zwracanie pustego/błędnego statusu
            throw new TpayCommunicationException("Cannot get transaction status - TPay auth error.", e, null);
        }


        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = transactionUrl + "/" + transactionId;

        try {
            ResponseEntity<TransactionStatusResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    TransactionStatusResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                LOG.info("Successfully fetched TPay transaction status for TPay ID: {}. Status: {}", transactionId, response.getBody().getStatus());
                return response.getBody();
            } else {
                LOG.warn("Failed to get TPay transaction status for TPay ID: {}. Status: {}, Body: {}",
                        transactionId, response.getStatusCode(), response.getBody());
                throw new TpayCommunicationException("TPay API returned non-2xx status for transaction status: " + response.getStatusCode(), response.getStatusCodeValue());
            }
        } catch (HttpStatusCodeException e) {
            LOG.error("HTTP error fetching TPay transaction status for TPay ID: {}. Status: {}, Response: {}",
                    transactionId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            // Jeśli TPay zwraca 404 dla nieistniejącej transakcji, można to zmapować na null lub specyficzny status
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOG.warn("TPay transaction with ID {} not found.", transactionId);
                // Zwróć odpowiedź, która to sygnalizuje lub rzuć ResourceNotFoundException
                TransactionStatusResponse notFoundResponse = new TransactionStatusResponse();
                notFoundResponse.setTransactionId(transactionId);
                notFoundResponse.setStatus("not_found"); // lub inny umowny status
                return notFoundResponse;
            }
            throw new TpayCommunicationException("HTTP error fetching TPay transaction status: " + e.getStatusCode(), e, e.getStatusCode().value());
        } catch (RestClientException e) {
            LOG.error("Error connecting to TPay for transaction status (TPay ID: {}): {}", transactionId, e.getMessage(), e);
            throw new TpayCommunicationException("Communication error while fetching TPay transaction status.", e);
        }
    }

    public void handleReservationCancellation(ReservationCancelledEvent event) {
        LOG.info("Handling reservation cancellation for ID: {}", event.getReservationId());
        Payment payment = paymentRepository.findByReservationId(event.getReservationId())
                .orElseGet(() -> {
                    LOG.warn("No payment found for cancelled reservation ID: {}. No compensation action needed in payment service.", event.getReservationId());
                    return null;
                });

        if (payment == null) {
            return;
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            LOG.info("Payment for reservation ID: {} is already '{}'. No further action.", event.getReservationId(), payment.getStatus());
            return;
        }

        // TODO: Implementacja logiki anulowania/zwrotu płatności w TPay API, jeśli to możliwe i wymagane.
        // Na przykład, jeśli płatność była "completed", a rezerwacja jest anulowana z powodu
        // błędu generowania biletu, może być potrzebny zwrot.
        // Obecnie tylko oznaczamy płatność jako anulowaną w naszej bazie.

        LOG.info("Marking payment as 'cancelled' for reservation ID: {}. TPay Transaction ID: {}",
                event.getReservationId(), payment.getTransactionId());
        payment.setStatus(PaymentStatus.CANCELLED); // Lub "pending_refund" jeśli jest proces zwrotu
        paymentRepository.save(payment);
        LOG.info("Payment for reservation ID: {} marked as cancelled.", event.getReservationId());
    }
}
