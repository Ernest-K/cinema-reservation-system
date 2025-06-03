package com.example.payment_service.controller;

import com.example.payment_service.service.PaymentService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.PaymentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<Page<PaymentDTO>> getAllPayments(
            @PageableDefault(size = 20, sort = "creationDate") Pageable pageable
    ) {
        Page<PaymentDTO> paymentsPage = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(paymentsPage);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDTO> getPaymentById(
            @PathVariable("paymentId")
            @NotNull(message = "Payment ID cannot be null.")
            @Positive(message = "Payment ID must be a positive number.")
            Long paymentId
    ) {
        PaymentDTO payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }
}