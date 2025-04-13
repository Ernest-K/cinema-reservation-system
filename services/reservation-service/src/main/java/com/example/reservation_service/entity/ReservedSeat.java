package com.example.reservation_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reserved_seats")
public class ReservedSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reservation_id")
    @ToString.Exclude
    private Reservation reservation;

    private Long seatId;
    private int rowNumber;
    private int seatNumber;
}