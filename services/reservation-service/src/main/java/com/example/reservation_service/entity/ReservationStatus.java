package com.example.reservation_service.entity;

public enum ReservationStatus {
    PENDING_PAYMENT,  // Oczekuje na płatność
    CONFIRMED,        // Potwierdzona (zapłacona)
    CANCELLED,        // Anulowana
    EXPIRED           // Wygasła (brak płatności w określonym czasie)
}
