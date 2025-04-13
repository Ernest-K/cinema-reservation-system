package com.example.movie_service.dto;

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
