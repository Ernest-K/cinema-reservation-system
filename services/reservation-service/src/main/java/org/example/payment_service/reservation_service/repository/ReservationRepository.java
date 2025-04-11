package org.example.payment_service.reservation_service.repository;

import org.example.payment_service.reservation_service.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserId(Long userId);

    @Query("SELECT r FROM Reservation r WHERE r.movieTitle = :movieTitle " +
            "AND r.screeningTime = :screeningTime " +
            "AND r.status = 'tymczasowa' " +
            "AND r.blockedUntil > :now")
    List<Reservation> findConflictingReservations(@Param("movieTitle") String movieTitle,
                                                  @Param("screeningTime") LocalDateTime screeningTime,
                                                  @Param("now") LocalDateTime now);

    List<Reservation> findByStatusAndBlockedUntilBefore(String status, LocalDateTime dateTime);
}
