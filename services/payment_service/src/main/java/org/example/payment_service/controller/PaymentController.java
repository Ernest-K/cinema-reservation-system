package org.example.payment_service.controller;

import org.example.payment_service.dto.PaymentRequest;
import org.example.payment_service.dto.PaymentResponse;
import org.example.payment_service.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = new PaymentResponse(request.getReservationId(), "sukces", "Refundacja wykonana.");
        return ResponseEntity.ok(response);
    }
}
