package org.example.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {
    private Long reservationId;
    private String paymentStatus;
    private String message;
}
