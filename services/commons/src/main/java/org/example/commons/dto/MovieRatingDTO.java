package org.example.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRatingDTO {
    private Long id;
    private String title;
    private int releaseYear;
    private double rating;
}
