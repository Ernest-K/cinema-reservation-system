package org.example.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.commons.enums.PaymentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusDTO {
    private Long paymentId;
    private Long reservationId;
    private PaymentStatus status;
}
