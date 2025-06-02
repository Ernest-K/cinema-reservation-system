package com.example.payment_service.repository;

import com.example.payment_service.entity.Payment;
import org.example.commons.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStatus(PaymentStatus status);
    Optional<Payment> findByReservationId(Long reservationId);
}
