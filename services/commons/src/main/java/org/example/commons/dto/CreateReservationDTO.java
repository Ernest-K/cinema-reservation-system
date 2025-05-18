package org.example.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationDTO {
    private Long movieId;
    private Long screeningId;
    private String customerName;
    private String customerEmail;
    private List<Long> seatIds;
}
