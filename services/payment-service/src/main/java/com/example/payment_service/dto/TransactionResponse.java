package com.example.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String result;
    private String requestId;
    private String transactionId;
    private String title;
    private String posId;
    private String status;
    private DateSection date;
    private double amount;
    private String currency;
    private String description;
    private String hiddenDescription;
    private Payer payer;
    private Payments payments;

    @JsonProperty("transactionPaymentUrl")
    private String transactionPaymentUrl;

    @Data
    public static class DateSection {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime creation;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime realization;
    }

    @Data
    public static class Payer {
        private String payerId;
        private String email;
        private String name;
        private String phone;
        private String address;
        private String city;
        private String country;
        private String postalCode;
    }

    @Data
    public static class Payments {
        private String status;
        private String method;
        private double amountPaid;
        private PaymentDate date;

        @Data
        public static class PaymentDate {
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime realization;
        }
    }
}
