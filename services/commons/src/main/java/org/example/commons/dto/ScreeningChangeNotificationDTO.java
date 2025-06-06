package org.example.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningChangeNotificationDTO {
    private String customerEmail;
    private String customerName;
    private String movieTitle;
    private Long reservationId;
    private Long originalScreeningId; // ID seansu, który uległ zmianie/anulowaniu

    // Informacje o zmianie
    private String changeType; // Np. "UPDATED", "CANCELLED"
    private String changeReason; // Np. powód anulowania

    // Stare dane (opcjonalnie, dla informacji w mailu)
    private LocalDateTime oldScreeningTime;
    private String oldHallInfo;

    // Nowe dane (jeśli UPDATED)
    private LocalDateTime newScreeningTime;
    private String newHallInfo;
    // Można dodać inne istotne szczegóły
}