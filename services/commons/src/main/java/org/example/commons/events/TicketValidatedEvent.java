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
public class TicketValidatedEvent {
    private Long ticketId;
    private String ticketUuid;
    private Long reservationId;
    private LocalDateTime validatedAt;
}