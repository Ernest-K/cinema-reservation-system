package com.example.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String result;
    private String transactionId;
    private String status;
    private String transactionPaymentUrl;
    private LocalDateTime creationDate;

    @Override
    public String toString() {
        return "TransactionResponse{" +
                "result='" + result + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", status='" + status + '\'' +
                ", transactionPaymentUrl='" + transactionPaymentUrl + '\'' +
                ", creationDate=" + creationDate +
                '}';
    }
}
