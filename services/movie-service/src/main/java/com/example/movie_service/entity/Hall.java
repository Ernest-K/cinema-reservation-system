package com.example.movie_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "halls")
public class Hall {

    public Hall(int number, int rows, int seatsPerRow) {
        this.number = number;
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        initializeStandardLayout();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int number;
    private int rows;
    private int seatsPerRow;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "hall_id")
    private List<Seat> seats = new ArrayList<>();

    public void initializeStandardLayout() {
        for (int row = 1; row <= this.rows; row++) {
            for (int seatNum = 1; seatNum <= this.seatsPerRow; seatNum++) {
                Seat seat = new Seat();
                seat.setRowNumber(row);
                seat.setSeatNumber(seatNum);
                seats.add(seat);
            }
        }
    }
}
