package org.example.commons.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationCancelledEvent {
    private Long reservationId;
    private String customerEmail;
    private String customerName;
    private String movieTitle;
    private LocalDateTime screeningStartTime;
    private String cancellationReason;
}
