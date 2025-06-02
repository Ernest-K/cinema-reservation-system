package com.example.notification_service.controller.advice;

import org.example.commons.exception.NotificationProcessingException;
import com.google.zxing.WriterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest; // Potrzebne dla kontekstu

import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotificationProcessingException.class)
    public void handleNotificationProcessingException(NotificationProcessingException ex, WebRequest request) {
        LOG.error("Notification Processing Exception: {}. Request: {}", ex.getMessage(), request.getDescription(false), ex);
    }

    @ExceptionHandler({IOException.class, WriterException.class})
    public void handleQrGenerationIssues(Exception ex, WebRequest request) {
        LOG.error("QR Code Generation Issue: {}. Request: {}", ex.getMessage(), request.getDescription(false), ex);
    }

    @ExceptionHandler(MailException.class) // Jeśli gdzieś MailException nie zostanie złapany przez @Recover
    public void handleMailException(MailException ex, WebRequest request) {
        LOG.error("Unhandled Mail Exception: {}. Request: {}", ex.getMessage(), request.getDescription(false), ex);
    }

    @ExceptionHandler(Exception.class)
    public void handleGenericException(Exception ex, WebRequest request) {
        LOG.error("Unhandled Generic Exception in Notification Service: {}. Request: {}", ex.getMessage(), request.getDescription(false), ex);
    }
}
