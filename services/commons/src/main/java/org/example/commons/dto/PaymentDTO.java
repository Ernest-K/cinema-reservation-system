package org.example.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.commons.enums.PaymentStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private Long id;
    private String tpayTransactionId;
    private Long reservationId;
    private PaymentStatus status;
    private LocalDateTime creationDate;
    private LocalDateTime expirationDate;
    private String paymentUrl;
}