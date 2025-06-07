package com.example.ticket_service.repository;

import com.example.ticket_service.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findById(Long ticketId);
    Optional<Ticket> findByReservationId(Long reservationId);
    List<Ticket> findAllByScreeningId(Long screeningId);
    Optional<Ticket> findTopByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    Optional<Ticket> findByTicketUuid(String ticketUuid);
}
