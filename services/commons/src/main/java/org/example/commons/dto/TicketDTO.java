package org.example.commons.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.commons.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {

    @NotNull(message = "Ticket ID cannot be null")
    @Positive(message = "Ticket ID must be positive")
    private Long id;
    private Long reservationId;
    private Long screeningId;
    private String movieTitle;
    private LocalDateTime screeningStartTime;
    private int hallNumber;
    private String customerName;

    @NotBlank(message = "Customer email cannot be blank in TicketDTO")
    @Email(message = "Invalid email format in TicketDTO")
    private String customerEmail;
    private BigDecimal totalPrice;
    private String seatsDescription;

    @NotBlank(message = "QR code data cannot be blank in TicketDTO")
    private String qrCodeData;
    private LocalDateTime createdAt;
    private LocalDateTime validatedAt;
    private TicketStatus status;
}
