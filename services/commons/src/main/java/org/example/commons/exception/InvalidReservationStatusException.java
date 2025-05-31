package org.example.commons.exception;

public class InvalidReservationStatusException extends RuntimeException {
    public InvalidReservationStatusException(String message) {
        super(message);
    }
}