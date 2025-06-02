package org.example.commons.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationDTO {

    @NotNull(message = "Movie ID cannot be null.")
    @Positive(message = "Movie ID must be a positive number.")
    private Long movieId;

    @NotNull(message = "Screening ID cannot be null.")
    @Positive(message = "Screening ID must be a positive number.")
    private Long screeningId;

    @NotBlank(message = "Customer name cannot be blank.")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters.")
    private String customerName;

    @NotBlank(message = "Customer email cannot be blank.")
    @Email(message = "Customer email should be a valid email address.")
    @Size(max = 100, message = "Customer email cannot exceed 100 characters.")
    private String customerEmail;

    @NotEmpty(message = "Seat IDs list cannot be empty.")
    private List<
            @NotNull(message = "Seat ID in list cannot be null.")
            @Positive(message = "Seat ID in list must be a positive number.")
                    Long
            > seatIds;
}
