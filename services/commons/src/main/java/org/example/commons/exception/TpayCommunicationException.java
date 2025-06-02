package org.example.commons.exception;

public class TpayCommunicationException extends RuntimeException {
    private final Integer statusCode;

    public TpayCommunicationException(String message) {
        super(message);
        this.statusCode = null;
    }

    public TpayCommunicationException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public TpayCommunicationException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public TpayCommunicationException(String message, Throwable cause, Integer statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
