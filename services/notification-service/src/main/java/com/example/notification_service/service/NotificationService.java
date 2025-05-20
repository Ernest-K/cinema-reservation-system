package com.example.notification_service.service;

import org.example.commons.dto.TicketDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import com.google.zxing.WriterException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class NotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
    private final JavaMailSender javaMailSender;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.sender}")
    private String senderEmail;

    private static final int QR_CODE_WIDTH = 250;
    private static final int QR_CODE_HEIGHT = 250;
    private static final String QR_IMAGE_RESOURCE_NAME = "ticketQrCode"; // Identyfikator dla CID

    public NotificationService(JavaMailSender javaMailSender, QrCodeGeneratorService qrCodeGeneratorService, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.templateEngine = templateEngine;
    }

    public void sendTicketNotificationEmail(TicketDTO ticketDTO) {
        LOG.info("Attempting to send ticket notification email to: {}", ticketDTO.getCustomerEmail());
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
            context.setVariable("qrImageResourceName", QR_IMAGE_RESOURCE_NAME); // Przekazanie nazwy zasobu do szablonu

            String htmlContent = templateEngine.process("ticket-email", context); // Nazwa pliku szablonu bez .html

            helper.setTo(ticketDTO.getCustomerEmail());
            helper.setFrom(senderEmail);
            helper.setSubject("Your Cinema Ticket for " + ticketDTO.getMovieTitle());
            helper.setText(htmlContent, true); // true oznacza, że treść to HTML

            // Dodanie obrazka QR jako zasobu inline (CID)
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
}
