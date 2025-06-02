package org.example.commons.exception;

public class TicketGenerationException extends RuntimeException {
    public TicketGenerationException(String message) {
        super(message);
    }

    public TicketGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}