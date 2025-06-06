package org.example.commons.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.commons.dto.ScreeningDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningCreatedEvent {
    private ScreeningDTO screeningDTO;
}