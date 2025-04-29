package com.example.reservation_service.repository;

import com.example.reservation_service.entity.ReservationStatus;
import com.example.reservation_service.entity.ReservedSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {
    List<ReservedSeat> findAllByReservationId(Long reservationId);
    void deleteAllByReservationId(Long reservationId);
    List<ReservedSeat> findByReservation_ScreeningId(Long screeningId);
    boolean existsBySeatIdAndReservation_ScreeningIdAndReservation_StatusNot(Long seatId, Long screeningId, ReservationStatus status);
}