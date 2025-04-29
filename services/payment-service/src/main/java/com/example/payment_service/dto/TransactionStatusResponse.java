package com.example.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatusResponse {
    public String result;
    public String requestId;
    public String transactionId;
    public String title;
    public String posId;
    public String status;
    public DateInfo date;
    public double amount;
    public String currency;
    public String description;
    public String hiddenDescription;
    public LockInfo lock;
    public PaymentsInfo payments;

    @Data
    public static class DateInfo {
        public String creation;
        public String realization;
    }

    @Data
    public static class LockInfo {
        public String type;
        public String status;
        public Double amount;
        public Double amountCollected;
    }

    @Data
    public static class PaymentsInfo {
        public List<String> attempts; // Można zamienić na List<Attempt> jeśli pojawią się szczegóły
    }
}
