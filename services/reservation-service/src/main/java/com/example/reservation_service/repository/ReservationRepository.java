package com.example.reservation_service.repository;

import com.example.reservation_service.entity.Reservation;
import org.example.commons.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByScreeningIdAndStatusIn(Long cancelledScreeningId, List<ReservationStatus> statusList);
}