package com.example.reservation_service.controller.advice;

import org.example.commons.dto.ErrorResponseDTO;
import org.example.commons.exception.InvalidReservationStatusException;
import org.example.commons.exception.ReservationConflictException;
import org.example.commons.exception.ResourceNotFoundException;
import org.example.commons.exception.ServiceCommunicationException;
import feign.FeignException; // Dla obsługi błędów Feign
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleReservationConflictException(
            ReservationConflictException ex, WebRequest request) {
        LOG.warn("Reservation conflict: {}", ex.getMessage());
        return new ResponseEntity<>(createErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidReservationStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidReservationStatusException(
            InvalidReservationStatusException ex, WebRequest request) {
        LOG.warn("Invalid reservation status for operation: {}", ex.getMessage());
        return new ResponseEntity<>(createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServiceCommunicationException.class)
    public ResponseEntity<ErrorResponseDTO> handleServiceCommunicationException(
            ServiceCommunicationException ex, WebRequest request) {
        LOG.error("Service communication error: {}", ex.getMessage(), ex.getCause());
        return new ResponseEntity<>(createErrorResponse(HttpStatus.BAD_GATEWAY, "Error communicating with a dependent service: " + ex.getMessage(), request), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(FeignException.class) // Obsługa błędów z Feign Client
    public ResponseEntity<ErrorResponseDTO> handleFeignException(FeignException ex, WebRequest request) {
        LOG.error("Feign client error. Status: {}, Method: {}, URL: {}, Message: {}",
                ex.status(), ex.request() != null ? ex.request().httpMethod() : "N/A",
                ex.request() != null ? ex.request().url() : "N/A", ex.contentUTF8(), ex);

        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR; // Domyślnie, jeśli status nieznany
        }
        String message = "Error communicating with service: " + ex.request().url() + ". Status: " + ex.status();
        if(ex.contentUTF8() != null && !ex.contentUTF8().isEmpty()){
            message += ". Details: " + ex.contentUTF8();
        }

        return new ResponseEntity<>(createErrorResponse(status, message, request), status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class) // Dla @Valid na @RequestBody
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        LOG.warn("Validation error on request body: {}", ex.getMessage());
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(createErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + errorMessage, request), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class) // Dla @Validated na parametrach kontrolera
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
