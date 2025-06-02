package com.example.payment_service.controller.advice;

import org.example.commons.dto.ErrorResponseDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.example.commons.exception.PaymentProcessingException;
import org.example.commons.exception.ResourceNotFoundException;
import org.example.commons.exception.TpayCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponseDTO createErrorResponse(HttpStatus status, String message, WebRequest request) {
        return new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", "")
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        LOG.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponseDTO> handlePaymentProcessingException(
            PaymentProcessingException ex, WebRequest request) {
        LOG.error("Payment processing error: {}", ex.getMessage(), ex.getCause());
        // Zazwyczaj błąd przetwarzania płatności to problem wewnętrzny lub nieudana operacja
        return new ResponseEntity<>(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Payment processing failed: " + ex.getMessage(), request), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TpayCommunicationException.class)
    public ResponseEntity<ErrorResponseDTO> handleTpayCommunicationException(
            TpayCommunicationException ex, WebRequest request) {
        LOG.error("TPay API communication error: {}. Status Code: {}", ex.getMessage(), ex.getStatusCode(), ex.getCause());
        HttpStatus status = ex.getStatusCode() != null ? HttpStatus.resolve(ex.getStatusCode()) : HttpStatus.BAD_GATEWAY;
        if (status == null) { // Jeśli resolve zwróci null
            status = HttpStatus.BAD_GATEWAY;
        }
        return new ResponseEntity<>(createErrorResponse(status, "Communication error with TPay: " + ex.getMessage(), request), status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        LOG.warn("Validation error on request body: {}", ex.getMessage());
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(createErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + errorMessage, request), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        LOG.warn("Constraint violation on request parameters: {}", ex.getMessage());
        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(createErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + errorMessage, request), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        LOG.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseEntity<>(createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(
            Exception ex, WebRequest request) {
        LOG.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred.", request), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}