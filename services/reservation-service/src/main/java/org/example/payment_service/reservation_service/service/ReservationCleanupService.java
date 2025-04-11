package org.example.payment_service.reservation_service.service;

import org.example.payment_service.reservation_service.entity.Reservation;
import org.example.payment_service.reservation_service.repository.ReservationRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@EnableScheduling
public class ReservationCleanupService {

    private final ReservationRepository reservationRepository;

    public ReservationCleanupService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Scheduled(fixedRate = 60000) // co 60 sekund
    public void releaseExpiredReservations() {
        List<Reservation> expired = reservationRepository
                .findByStatusAndBlockedUntilBefore("tymczasowa", LocalDateTime.now());

        for (Reservation res : expired) {
            res.setStatus("anulowana"); // lub "expired"
        }
        reservationRepository.saveAll(expired);
    }
}
