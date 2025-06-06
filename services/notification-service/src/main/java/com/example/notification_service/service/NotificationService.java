package com.example.notification_service.service;

import com.example.notification_service.entity.Notification;
import com.example.notification_service.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.commons.dto.ScreeningChangeNotificationDTO;
import org.example.commons.dto.TicketDTO;
import org.example.commons.enums.NotificationStatus;
import org.example.commons.events.ReservationCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import com.google.zxing.WriterException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
    private final JavaMailSender javaMailSender;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final TemplateEngine templateEngine;
    private final NotificationRepository notificationRepository;

    @Value("${app.mail.sender}")
    private String senderEmail;

    private static final int QR_CODE_WIDTH = 250;
    private static final int QR_CODE_HEIGHT = 250;
    private static final String QR_IMAGE_RESOURCE_NAME = "ticketQrCode";
    private static final String NOTIFICATION_TYPE_TICKET_CONFIRMATION = "TICKET_CONFIRMATION_EMAIL";
    private static final String NOTIFICATION_TYPE_SCREENING_CHANGE = "SCREENING_CHANGE_EMAIL";
    private static final String NOTIFICATION_TYPE_RESERVATION_CANCELLED = "RESERVATION_CANCELLED_EMAIL";

    public void processAndSendScreeningChangeNotification(ScreeningChangeNotificationDTO payload) {
        Notification log = createInitialLogForScreeningChange(payload);
        try {
            sendScreeningChangeEmailWithRetry(payload, log);
            updateNotificationStatus(log, NotificationStatus.SENT, null);
        } catch (Exception e) {
            LOG.error("Exception after attempting to send screening change email for reservation ID {}: {}", payload.getReservationId(), e.getMessage());
            if (log.getStatus() != NotificationStatus.FAILED_FINAL && log.getStatus() != NotificationStatus.FAILED_RETRY) {
                updateNotificationStatus(log, NotificationStatus.FAILED_FINAL, e.getMessage());
            }
        }
    }

    @Retryable(
            retryFor = {MailException.class, MessagingException.class},
            maxAttemptsExpression = "${app.mail.retry.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.mail.retry.delay:2000}", multiplierExpression = "${app.mail.retry.multiplier:2}")
    )
    public void sendScreeningChangeEmailWithRetry(ScreeningChangeNotificationDTO payload, Notification log)
            throws MailException, MessagingException {
        RetryContext retryContext = RetrySynchronizationManager.getContext();
        int currentAttempt = retryContext != null ? retryContext.getRetryCount() + 1 : 1;

        LOG.info("Attempting to send screening change notification to: {} for reservation ID: {} (Attempt: {})",
                payload.getCustomerEmail(), payload.getReservationId(), currentAttempt);

        log.setAttemptCount(currentAttempt);
        log.setLastAttemptAt(LocalDateTime.now());
        log.setStatus(currentAttempt > 1 ? NotificationStatus.FAILED_RETRY : NotificationStatus.PENDING);
        notificationRepository.save(log);

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name()); // Prostszy helper, bez obrazków

        Context context = new Context();
        context.setVariable("customerName", payload.getCustomerName());
        context.setVariable("movieTitle", payload.getMovieTitle());
        context.setVariable("reservationId", payload.getReservationId());
        context.setVariable("changeType", payload.getChangeType());
        context.setVariable("changeReason", payload.getChangeReason());
        context.setVariable("oldScreeningTime", payload.getOldScreeningTime());
        context.setVariable("oldHallInfo", payload.getOldHallInfo());
        if ("UPDATED".equals(payload.getChangeType())) {
            context.setVariable("newScreeningTime", payload.getNewScreeningTime());
            context.setVariable("newHallInfo", payload.getNewHallInfo());
        }

        String htmlContent = templateEngine.process("screening-change-email", context);

        helper.setTo(payload.getCustomerEmail());
        helper.setFrom(senderEmail);
        helper.setSubject("Important Update Regarding Your Reservation for " + payload.getMovieTitle());
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
        LOG.info("Screening change notification email sent successfully to: {} for reservation ID: {}",
                payload.getCustomerEmail(), payload.getReservationId());
    }

    // Metody @Recover dla ScreeningChangeNotificationDTO
    @Recover
    public void recoverScreeningChangeEmail(MailException e, ScreeningChangeNotificationDTO payload, Notification log) {
        LOG.error("Failed to send screening change notification to {} for reservation ID {} after all retries. Error: {}",
                payload.getCustomerEmail(), payload.getReservationId(), e.getMessage());
        updateNotificationStatus(log, NotificationStatus.FAILED_FINAL, e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    @Recover
    public void recoverScreeningChangeEmail(MessagingException e, ScreeningChangeNotificationDTO payload, Notification log) {
        LOG.error("Failed to send screening change notification to {} for reservation ID {} after all retries (MessagingException). Error: {}",
                payload.getCustomerEmail(), payload.getReservationId(), e.getMessage());
        updateNotificationStatus(log, NotificationStatus.FAILED_FINAL, "MessagingException: " + e.getMessage());
    }

    @Transactional
    public Notification createInitialLogForScreeningChange(ScreeningChangeNotificationDTO payload) {
        Notification log = Notification.builder()
                .reservationId(payload.getReservationId()) // Kluczowe ID
                .ticketId(null) // Nie dotyczy konkretnego biletu, a rezerwacji i zmiany seansu
                .customerEmail(payload.getCustomerEmail())
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .lastAttemptAt(LocalDateTime.now())
                .attemptCount(0)
                .notificationType(NOTIFICATION_TYPE_SCREENING_CHANGE)
                .lastErrorMessage("Change Type: " + payload.getChangeType() + ", Screening: " + payload.getOriginalScreeningId()) // Dodatkowe info
                .build();
        return notificationRepository.save(log);
    }

    public void processAndSendTicketNotification(TicketDTO ticketDTO) {
        // Sprawdzenie idempotentności na podstawie logu
        // To proste sprawdzenie, można je rozbudować (np. status != SENT)
        if (notificationRepository.findByTicketIdAndNotificationType(ticketDTO.getId(), NOTIFICATION_TYPE_TICKET_CONFIRMATION)
                .filter(log -> log.getStatus() == NotificationStatus.SENT || log.getStatus() == NotificationStatus.PENDING && log.getAttemptCount() > 0) // Jeśli PENDING i już próbowano, to @Retryable działa
                .isPresent()) {
            LOG.warn("Notification for ticket ID {} of type {} already processed or in progress. Skipping.", ticketDTO.getId(), NOTIFICATION_TYPE_TICKET_CONFIRMATION);
            return;
        }

        Notification log = createInitialNotification(ticketDTO);
        try {
            sendTicketNotificationEmailWithRetry(ticketDTO, log);
            // Jeśli doszło tutaj bez wyjątku z @Retryable (rzadkie, bo @Recover powinien złapać)
            // lub jeśli @Retryable się powiodło od razu.
            updateNotificationStatus(log, NotificationStatus.SENT, null);
        } catch (Exception e) { // Łapanie wyjątków, które mogłyby wyjść z @Retryable (np. nieobjęte przez retryFor)
            // lub jeśli @Recover rzuciłby dalej.
            LOG.error("Exception after attempting to send email for ticket ID {} (possibly after retries): {}", ticketDTO.getId(), e.getMessage());
            // Status logu będzie ustawiony w @Recover lub tutaj jeśli błąd nie jest mailowy
            if (log.getStatus() != NotificationStatus.FAILED_FINAL && log.getStatus() != NotificationStatus.FAILED_RETRY) {
                updateNotificationStatus(log, NotificationStatus.FAILED_FINAL, e.getMessage());
            }
        }
    }

    @Retryable(
            retryFor = {MailException.class, MessagingException.class}, // Typy wyjątków, które mają wywołać ponowienie
            maxAttempts = 3,                                        // Maksymalna liczba prób (pierwsza + 2 ponowienia)
            backoff = @Backoff(delay = 2000, multiplier = 2)        // Opóźnienie między próbami (2s, 4s)
    )
    public void sendTicketNotificationEmailWithRetry(TicketDTO ticketDTO, Notification notification)
            throws MailException, MessagingException, IOException, WriterException {

        RetryContext retryContext = RetrySynchronizationManager.getContext();
        int currentAttempt = retryContext != null ? retryContext.getRetryCount() + 1 : 1;

        LOG.info("Attempting to send ticket notification email to: {} for ticket ID: {} (Attempt: {})",
                ticketDTO.getCustomerEmail(), ticketDTO.getId(), currentAttempt);

        // Aktualizuj log przed próbą
        notification.setAttemptCount(currentAttempt);
        notification.setLastAttemptAt(LocalDateTime.now());
        notification.setStatus(NotificationStatus.PENDING); // Lub FAILED_RETRY, jeśli to nie pierwsza próba
        if (currentAttempt > 1) {
            notification.setStatus(NotificationStatus.FAILED_RETRY);
        }
        notificationRepository.save(notification); // Zapis w tej samej transakcji co wysyłka


        if (ticketDTO.getCustomerEmail() == null || ticketDTO.getCustomerEmail().isEmpty()) {
            updateNotificationStatus(notification, NotificationStatus.SKIPPED, "Customer email is missing.");
            LOG.error("Customer email is missing for ticket ID: {}. Notification skipped.", ticketDTO.getId());
            throw new IllegalArgumentException("Customer email is missing in TicketDTO.");
        }
        if (ticketDTO.getQrCodeData() == null || ticketDTO.getQrCodeData().isEmpty()) {
            updateNotificationStatus(notification, NotificationStatus.SKIPPED, "QR code data is missing.");
            LOG.error("QR code data is missing for ticket ID: {}. Notification skipped.", ticketDTO.getId());
            throw new IllegalArgumentException("QR code data is missing in TicketDTO.");
        }

        byte[] qrImageBytes;
        try {
            qrImageBytes = qrCodeGeneratorService.generateQrCodeImage(
                    ticketDTO.getQrCodeData(), QR_CODE_WIDTH, QR_CODE_HEIGHT
            );
        } catch (WriterException | IOException e) {
            LOG.error("Error generating QR code image for ticket ID {}: {}", ticketDTO.getId(), e.getMessage(), e);
            updateNotificationStatus(notification, NotificationStatus.FAILED_FINAL, "QR generation failed: " + e.getMessage());
            throw e; // Rzucamy dalej, aby @Recover mógł to złapać, jeśli jest skonfigurowany dla tych wyjątków
            // lub aby konsumer to obsłużył
        }

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        Context context = new Context();
        context.setVariable("customerName", ticketDTO.getCustomerName());
        // ... (ustawianie reszty zmiennych kontekstu jak poprzednio) ...
        context.setVariable("movieTitle", ticketDTO.getMovieTitle());
        context.setVariable("screeningTime", ticketDTO.getScreeningStartTime());
        context.setVariable("hallInfo", "Hall " + ticketDTO.getHallNumber());
        context.setVariable("seatsInfo", ticketDTO.getSeatsDescription());
        context.setVariable("reservationId", ticketDTO.getReservationId());
        context.setVariable("qrImageResourceName", QR_IMAGE_RESOURCE_NAME);

        String htmlContent = templateEngine.process("ticket-email", context);

        helper.setTo(ticketDTO.getCustomerEmail());
        helper.setFrom(senderEmail);
        helper.setSubject("Your Cinema Ticket for " + ticketDTO.getMovieTitle() + " (Ticket ID: " + ticketDTO.getId() + ")");
        helper.setText(htmlContent, true);
        ByteArrayResource qrImageResource = new ByteArrayResource(qrImageBytes);
        helper.addInline(QR_IMAGE_RESOURCE_NAME, qrImageResource, "image/png");

        javaMailSender.send(mimeMessage); // Może rzucić MailException

        LOG.info("Ticket notification email tentative send successful to: {} for ticket ID: {}",
                ticketDTO.getCustomerEmail(), ticketDTO.getId());
    }

    @Recover
    public void recoverEmailSending(MailException e, TicketDTO ticketDTO, Notification notification) {
        LOG.error("Failed to send ticket notification email to {} after multiple retries. Payload: {}. Error: {}",
                ticketDTO.getCustomerEmail(), ticketDTO, e.getMessage(), e);
        updateNotificationStatus(notification, NotificationStatus.FAILED_FINAL, e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    @Recover
    public void recoverEmailSending(MessagingException e, TicketDTO ticketDTO, Notification notification) {
        LOG.error("Failed to send ticket notification email to {} after multiple retries due to MessagingException. Payload: {}. Error: {}",
                ticketDTO.getCustomerEmail(), ticketDTO, e.getMessage(), e);
        updateNotificationStatus(notification, NotificationStatus.FAILED_FINAL, "MessagingException: " + e.getMessage());
    }

    @Transactional
    public Notification createInitialNotification(TicketDTO ticketDTO) {
        Notification log = Notification.builder()
                .ticketId(ticketDTO.getId())
                .reservationId(ticketDTO.getReservationId())
                .customerEmail(ticketDTO.getCustomerEmail())
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .lastAttemptAt(LocalDateTime.now())
                .attemptCount(0)
                .notificationType(NOTIFICATION_TYPE_TICKET_CONFIRMATION)
                .build();
        return notificationRepository.save(log);
    }

    @Transactional
    public void updateNotificationStatus(Notification log, NotificationStatus status, String errorMessage) {
        log.setStatus(status);
        log.setLastErrorMessage(errorMessage);
        if (status == NotificationStatus.SENT) {
            log.setSentAt(LocalDateTime.now());
        }
        log.setLastAttemptAt(LocalDateTime.now());
        notificationRepository.save(log);
    }

    public void processAndSendReservationCancelledNotification(ReservationCancelledEvent event) {
        if (notificationRepository.findByReservationIdAndNotificationType(event.getReservationId(), NOTIFICATION_TYPE_RESERVATION_CANCELLED)
                .filter(log -> log.getStatus() == NotificationStatus.SENT)
                .isPresent()) {
            LOG.warn("Cancellation notification for reservation ID {} (type {}) already sent. Skipping.",
                    event.getReservationId(), NOTIFICATION_TYPE_RESERVATION_CANCELLED);
            return;
        }

        Notification log = createInitialLogForCancellation(event);
        try {
            sendReservationCancelledEmailWithRetry(event, log);
            updateNotificationStatus(log, NotificationStatus.SENT, null);
        } catch (Exception e) {
            LOG.error("Exception after attempting to send cancellation email for reservation ID {}: {}",
                    event.getReservationId(), e.getMessage());
            if (log.getStatus() != NotificationStatus.FAILED_FINAL && log.getStatus() != NotificationStatus.FAILED_RETRY) {
                updateNotificationStatus(log, NotificationStatus.FAILED_FINAL, e.getMessage());
            }
        }
    }

    @Transactional
    public Notification createInitialLogForCancellation(ReservationCancelledEvent event) {
        Notification log = Notification.builder()
                .reservationId(event.getReservationId())
                .ticketId(null)
                .customerEmail(event.getCustomerEmail())
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .lastAttemptAt(LocalDateTime.now())
                .attemptCount(0)
                .notificationType(NOTIFICATION_TYPE_RESERVATION_CANCELLED)
                .build();
        return notificationRepository.save(log);
    }

    @Transactional
    public void logSkippedCancellationNotification(ReservationCancelledEvent event, String reason) {
        if (notificationRepository.findByReservationIdAndNotificationType(event.getReservationId(), NOTIFICATION_TYPE_RESERVATION_CANCELLED).isEmpty()) {
            Notification log = Notification.builder()
                    .reservationId(event.getReservationId())
                    .customerEmail(event.getCustomerEmail())
                    .status(NotificationStatus.SKIPPED)
                    .createdAt(LocalDateTime.now())
                    .lastAttemptAt(LocalDateTime.now())
                    .attemptCount(0)
                    .lastErrorMessage(reason)
                    .notificationType(NOTIFICATION_TYPE_RESERVATION_CANCELLED)
                    .build();
            notificationRepository.save(log);
            LOG.info("Logged SKIPPED cancellation notification for reservation ID: {}", event.getReservationId());
        }
    }


    @Retryable( /* ... jak poprzednio ... */ )
    public void sendReservationCancelledEmailWithRetry(ReservationCancelledEvent event, Notification log)
            throws MailException, MessagingException {
        // ... (logika pobierania currentAttempt, aktualizacji logu) ...

        // Sprawdź, czy email istnieje, chociaż konsumer już to powinien zrobić
        if (event.getCustomerEmail() == null || event.getCustomerEmail().isEmpty()) {
            updateNotificationStatus(log, NotificationStatus.SKIPPED, "Customer email is missing for cancellation.");
            throw new IllegalArgumentException("Customer email is missing for cancellation notification.");
        }

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());

        Context context = new Context();
        context.setVariable("customerName", event.getCustomerName()); // Użyj pola z eventu
        context.setVariable("reservationId", event.getReservationId());
        context.setVariable("movieTitle", event.getMovieTitle());
        context.setVariable("screeningStartTime", event.getScreeningStartTime());
        context.setVariable("cancellationReason", event.getCancellationReason());

        String htmlContent = templateEngine.process("reservation-cancelled-email", context); // Ten sam szablon

        helper.setTo(event.getCustomerEmail());
        helper.setFrom(senderEmail);
        helper.setSubject("Confirmation of Your Reservation Cancellation (ID: " + event.getReservationId() + ")");
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
        LOG.info("Reservation cancellation email sent (attempt {}) to: {} for reservation ID: {}",
                log.getAttemptCount(), event.getCustomerEmail(), event.getReservationId());
    }

    @Recover
    public void recoverCancellationEmailSending(MailException e, ReservationCancelledEvent event, Notification log) {
        LOG.error("Failed to send reservation cancellation email to {} for reservation ID {} after all retries. Final Error: {}",
                event.getCustomerEmail(), event.getReservationId(), e.getMessage());
        updateNotificationStatus(log, NotificationStatus.FAILED_FINAL, e.getClass().getSimpleName() + ": " + e.getMessage());
    }
    @Recover
    public void recoverCancellationEmailSending(MessagingException e, ReservationCancelledEvent event, Notification log) {
        LOG.error("Failed to send reservation cancellation email to {} for reservation ID {} after all retries. Final Error: {}",
                event.getCustomerEmail(), event.getReservationId(), e.getMessage());
        updateNotificationStatus(log, NotificationStatus.FAILED_FINAL, "MessagingException: " + e.getMessage());
    }
}
