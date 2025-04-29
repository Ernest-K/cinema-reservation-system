package com.example.payment_service.controller;

import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/hello")
    public String hello() {

        paymentService.getAccessToken();
        return "Hello";
    }
}