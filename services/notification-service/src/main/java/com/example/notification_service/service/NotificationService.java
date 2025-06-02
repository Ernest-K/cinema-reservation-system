package com.example.notification_service.service;

import lombok.RequiredArgsConstructor;
import org.example.commons.dto.TicketDTO;
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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
    private final JavaMailSender javaMailSender;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.sender}")
    private String senderEmail;

    private static final int QR_CODE_WIDTH = 250;
    private static final int QR_CODE_HEIGHT = 250;
    private static final String QR_IMAGE_RESOURCE_NAME = "ticketQrCode";

    @Retryable(
            retryFor = {MailException.class, MessagingException.class}, // Typy wyjątków, które mają wywołać ponowienie
            maxAttempts = 3,                                        // Maksymalna liczba prób (pierwsza + 2 ponowienia)
            backoff = @Backoff(delay = 2000, multiplier = 2)        // Opóźnienie między próbami (2s, 4s)
    )
    public void sendTicketNotificationEmail(TicketDTO ticketDTO) {
        LOG.info("Attempting to send ticket notification email to: {}", ticketDTO.getCustomerEmail());

        if (ticketDTO.getCustomerEmail() == null || ticketDTO.getCustomerEmail().isEmpty()) {
            LOG.error("Customer email is missing for ticket ID: {}. Cannot send notification.", ticketDTO.getId());
            throw new IllegalArgumentException("Customer email is missing in TicketDTO.");
        }
        if (ticketDTO.getQrCodeData() == null || ticketDTO.getQrCodeData().isEmpty()) {
            LOG.error("QR code data is missing for ticket ID: {}. Cannot send notification.", ticketDTO.getId());
            throw new IllegalArgumentException("QR code data is missing in TicketDTO.");
        }

        try {
            byte[] qrImageBytes = qrCodeGeneratorService.generateQrCodeImage(
                    ticketDTO.getQrCodeData(), QR_CODE_WIDTH, QR_CODE_HEIGHT
            );

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, // Ważne dla osadzania obrazków
                    StandardCharsets.UTF_8.name()
            );

            // Przygotowanie kontekstu dla Thymeleaf
            Context context = new Context();
            context.setVariable("customerName", ticketDTO.getCustomerName());
            context.setVariable("movieTitle", ticketDTO.getMovieTitle());
            context.setVariable("screeningTime", ticketDTO.getScreeningStartTime());
            context.setVariable("hallInfo", ticketDTO.getHallNumber());
            context.setVariable("seatsInfo", ticketDTO.getSeatsDescription());
            context.setVariable("reservationId", ticketDTO.getReservationId());
            context.setVariable("qrImageResourceName", QR_IMAGE_RESOURCE_NAME);

            String htmlContent = templateEngine.process("ticket-email", context);

            helper.setTo(ticketDTO.getCustomerEmail());
            helper.setFrom(senderEmail);
            helper.setSubject("Your Cinema Ticket for " + ticketDTO.getMovieTitle());
            helper.setText(htmlContent, true);

            ByteArrayResource qrImageResource = new ByteArrayResource(qrImageBytes);
            helper.addInline(QR_IMAGE_RESOURCE_NAME, qrImageResource, "image/png");

            javaMailSender.send(mimeMessage);
            LOG.info("Ticket notification email sent successfully to: {}", ticketDTO.getCustomerEmail());

        } catch (WriterException | IOException e) {
            LOG.error("Error generating QR code for email to {}: {}", ticketDTO.getCustomerEmail(), e.getMessage(), e);
            // Rozważ strategię ponawiania lub powiadamiania administratora
        } catch (MessagingException e) {
            LOG.error("Error sending email to {}: {}", ticketDTO.getCustomerEmail(), e.getMessage(), e);
            // Rozważ strategię ponawiania lub powiadamiania administratora
        } catch (Exception e) {
            LOG.error("An unexpected error occurred while sending email to {}: {}", ticketDTO.getCustomerEmail(), e.getMessage(), e);
        }
    }

    @Recover
    public void recoverEmailSending(MailException e, TicketDTO ticketDTO) {
        LOG.error("Failed to send ticket notification email to {} after multiple retries. Payload: {}. Error: {}",
                ticketDTO.getCustomerEmail(), ticketDTO, e.getMessage(), e);
    }

    @Recover
    public void recoverEmailSending(MessagingException e, TicketDTO ticketDTO) {
        LOG.error("Failed to send ticket notification email to {} after multiple retries due to MessagingException. Payload: {}. Error: {}",
                ticketDTO.getCustomerEmail(), ticketDTO, e.getMessage(), e);
    }
}
