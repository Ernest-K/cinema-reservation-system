package org.example.commons.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketGenerationFailedEvent {
    private Long reservationId;
    private String reason;
}
