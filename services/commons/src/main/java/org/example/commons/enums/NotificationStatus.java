package org.example.commons.enums;

public enum NotificationStatus {
    PENDING,        // Oczekuje na wysłanie
    SENT,           // Pomyślnie wysłano
    FAILED_RETRY,   // Nieudana próba, będzie ponawiana (przez Spring Retry)
    FAILED_FINAL,   // Ostatecznie nieudane po wszystkich próbach
    SKIPPED         // Pominięto (np. email niepoprawny, duplikat)
}