package org.example.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallDTO {
    private Long id;
    private int number;
    private int rows;
    private int seatsPerRow;
}
