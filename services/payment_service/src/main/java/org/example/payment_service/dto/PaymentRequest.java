package org.example.payment_service.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long reservationId;
    private Double amount;
    private String blikCode;
}
