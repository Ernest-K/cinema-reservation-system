package com.example.payment_service.repository;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}