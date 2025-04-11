package org.example.payment_service.service;

import org.example.payment_service.dto.PaymentRequest;
import org.example.payment_service.dto.PaymentResponse;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {


    public PaymentResponse processPayment(PaymentRequest request) {

        if (request.getBlikCode() != null && request.getBlikCode().matches("\\d{6}")) {
            return new PaymentResponse(request.getReservationId(), "sukces", "Płatność została przetworzona pomyślnie.");
        } else {
            return new PaymentResponse(request.getReservationId(), "niepowodzenie", "Błędny kod BLIK.");
        }
    }
}
