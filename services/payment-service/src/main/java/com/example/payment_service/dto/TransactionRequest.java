package com.example.payment_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    private BigDecimal amount;
    private String description;
    private Payer payer;
    private Pay pay;

    @Data
    public static class Payer {
        private String email;
        private String name;
    }

    @Data
    public static class Pay {
        private int groupId;
    }
}

