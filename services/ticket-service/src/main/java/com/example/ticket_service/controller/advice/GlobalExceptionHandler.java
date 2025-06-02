package com.example.ticket_service.controller.advice;

import com.google.zxing.WriterException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.example.commons.dto.ErrorResponseDTO;
import org.example.commons.exception.ResourceNotFoundException;
import org.example.commons.exception.TicketAlreadyExistsException;
import org.example.commons.exception.TicketGenerationException;
import org.example.commons.exception.TicketValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
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

    @ExceptionHandler(TicketAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleTicketAlreadyExistsException(
            TicketAlreadyExistsException ex, WebRequest request) {
        LOG.warn("Ticket already exists: {}", ex.getMessage());
        return new ResponseEntity<>(createErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TicketGenerationException.class)
    public ResponseEntity<ErrorResponseDTO> handleTicketGenerationException(
            TicketGenerationException ex, WebRequest request) {
        LOG.error("Ticket generation error: {}", ex.getMessage(), ex.getCause());
        return new ResponseEntity<>(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error during ticket generation: " + ex.getMessage(), request), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TicketValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleTicketValidationException(
            TicketValidationException ex, WebRequest request) {
        LOG.warn("Ticket validation error: {}", ex.getMessage());
        // Często błędy walidacji (np. bilet już zwalidowany) to 409 Conflict lub 400 Bad Request
        return new ResponseEntity<>(createErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({IOException.class, WriterException.class}) // Błędy generowania QR
    public ResponseEntity<ErrorResponseDTO> handleQrCodeGenerationException(
            Exception ex, WebRequest request) {
        LOG.error("QR Code generation failed: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate QR code image.", request), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class) // Np. naruszenie unique constraint
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        LOG.warn("Data integrity violation: {}", ex.getMessage(), ex);
        // Może to być spowodowane próbą utworzenia duplikatu, gdzie jest unique constraint
        return new ResponseEntity<>(createErrorResponse(HttpStatus.CONFLICT, "Data integrity issue, possibly a duplicate entry.", request), HttpStatus.CONFLICT);
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

    @ExceptionHandler(IllegalStateException.class) // np. brak danych QR w bilecie
    public ResponseEntity<ErrorResponseDTO> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        LOG.error("Illegal state encountered: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal state error occurred: " + ex.getMessage(), request), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(
            Exception ex, WebRequest request) {
        LOG.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred.", request), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}