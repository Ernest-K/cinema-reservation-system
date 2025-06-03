package com.example.ticket_service.service;

import com.example.ticket_service.kafka.producer.MessageProducer;
import org.example.commons.dto.*;
import org.example.commons.enums.TicketStatus;
import org.example.commons.events.TicketGenerationFailedEvent;
import com.example.ticket_service.entity.Ticket;
import com.example.ticket_service.repository.TicketRepository;
import com.google.zxing.WriterException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.commons.exception.ResourceNotFoundException;
import org.example.commons.exception.TicketAlreadyExistsException;
import org.example.commons.exception.TicketGenerationException;
import org.example.commons.exception.TicketValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketService.class);
    private final MessageProducer producer;
    private final TicketRepository ticketRepository;
    private final QrCodeGeneratorService qrCodeGeneratorService;

    @Transactional
    public Ticket generateAndSaveTicketForReservation(ReservationDTO reservationDTO) {
        LOG.info("Attempting to generate ticket for reservation ID: {}", reservationDTO.getId());

        if (reservationDTO == null || reservationDTO.getId() == null) {
            LOG.error("Invalid ReservationDTO received: null or missing ID.");
            throw new TicketGenerationException("Invalid reservation data provided.");
        }

        try {
            Optional<Ticket> existingTicketOpt = ticketRepository.findByReservationId(reservationDTO.getId());
            if (existingTicketOpt.isPresent()) {
                LOG.warn("Ticket for reservation ID: {} already exists. ID: {}. Skipping generation.",
                        reservationDTO.getId(), existingTicketOpt.get().getId());
                throw new TicketAlreadyExistsException("Ticket for reservation ID " + reservationDTO.getId() + " already exists.");
            }
        } catch (Exception e) {
            LOG.error("Database error while checking for existing ticket for reservation ID {}: {}", reservationDTO.getId(), e.getMessage(), e);
            producer.sendTicketGenerationFailed(new TicketGenerationFailedEvent(reservationDTO.getId(), "Database error during ticket pre-check."));
            throw new TicketGenerationException("Error checking for existing ticket.", e);
        }

        if (reservationDTO.getSeats() == null || reservationDTO.getSeats().isEmpty()) {
            LOG.warn("No seats found in reservation DTO for reservation ID: {}. Cannot generate ticket.", reservationDTO.getId());
            producer.sendTicketGenerationFailed(new TicketGenerationFailedEvent(reservationDTO.getId(), "No seats provided in reservation."));
            throw new TicketGenerationException("No seats provided in reservation for ID: " + reservationDTO.getId());
        }
        if (reservationDTO.getScreeningDTO() == null || reservationDTO.getScreeningDTO().getMovieDTO() == null || reservationDTO.getScreeningDTO().getHallDTO() == null) {
            LOG.warn("Incomplete screening information in reservation DTO for reservation ID: {}.", reservationDTO.getId());
            producer.sendTicketGenerationFailed(new TicketGenerationFailedEvent(reservationDTO.getId(), "Incomplete screening information."));
            throw new TicketGenerationException("Incomplete screening information for reservation ID: " + reservationDTO.getId());
        }

        ScreeningDTO screening = reservationDTO.getScreeningDTO();
        String seatsDescriptionAggregated = reservationDTO.getSeats().stream()
                .map(seat -> "R" + seat.getRowNumber() + "S" + seat.getSeatNumber())
                .collect(Collectors.joining(", "));

        Ticket ticket = Ticket.builder()
                .reservationId(reservationDTO.getId())
                .screeningId(screening.getId())
                .movieTitle(screening.getMovieDTO().getTitle())
                .screeningStartTime(screening.getStartTime())
                .hallId(screening.getHallDTO().getId())
                .hallNumber(screening.getHallDTO().getNumber())
                .customerName(reservationDTO.getCustomerName())
                .customerEmail(reservationDTO.getCustomerEmail())
                .price(reservationDTO.getTotalAmount())
                .qrCodeData("")
                .seatsDescription(seatsDescriptionAggregated)
                .createdAt(LocalDateTime.now())
                .status(TicketStatus.VALID)
                .build();

        Ticket savedTicketWithId;
        try {
            savedTicketWithId = ticketRepository.saveAndFlush(ticket);
        } catch (DataIntegrityViolationException e) {
            LOG.error("Data integrity violation while saving ticket for reservation ID {}: {}. This might indicate a race condition or a duplicate reservation ID constraint.",
                    reservationDTO.getId(), e.getMessage(), e);
            producer.sendTicketGenerationFailed(new TicketGenerationFailedEvent(reservationDTO.getId(), "Data integrity error saving ticket."));
            throw new TicketAlreadyExistsException("Failed to save ticket due to data integrity issue, possibly duplicate for reservation ID " + reservationDTO.getId() + ".");
        } catch (Exception e) {
            LOG.error("Error saving initial ticket record for reservation ID {}: {}", reservationDTO.getId(), e.getMessage(), e);
            producer.sendTicketGenerationFailed(new TicketGenerationFailedEvent(reservationDTO.getId(), "Database error saving ticket."));
            throw new TicketGenerationException("Error saving ticket record.", e);
        }


        List<String> seatsInfoListForQr = reservationDTO.getSeats().stream()
                .map(seat -> "Row " + seat.getRowNumber() + ", Seat " + seat.getSeatNumber())
                .collect(Collectors.toList());

        QrCodePayloadDTO qrPayload = new QrCodePayloadDTO(
                savedTicketWithId.getId(),
                reservationDTO.getId(),
                screening.getMovieDTO().getTitle(),
                screening.getStartTime(),
                "Hall " + screening.getHallDTO().getNumber(),
                seatsInfoListForQr,
                reservationDTO.getCustomerName(),
                reservationDTO.getSeats().size()
        );

        String qrCodeText;
        try {
            qrCodeText = qrCodeGeneratorService.generateQrCodeText(qrPayload);
        } catch (RuntimeException e) {
            LOG.error("Failed to generate QR code text for ticket ID {}: {}", savedTicketWithId.getId(), e.getMessage(), e);
            producer.sendTicketGenerationFailed(new TicketGenerationFailedEvent(reservationDTO.getId(), "QR code text generation failed."));
            throw new TicketGenerationException("Failed to generate QR code text for ticket " + savedTicketWithId.getId(), e);
        }

        savedTicketWithId.setQrCodeData(qrCodeText);
        Ticket finalTicket;
        try {
            finalTicket = ticketRepository.save(savedTicketWithId);
        } catch (Exception e) {
            LOG.error("Error saving ticket with QR code data for reservation ID {}: {}", reservationDTO.getId(), e.getMessage(), e);
            producer.sendTicketGenerationFailed(new TicketGenerationFailedEvent(reservationDTO.getId(), "Database error saving ticket with QR code."));
            throw new TicketGenerationException("Error saving ticket with QR code.", e);
        }

        LOG.info("Successfully generated and saved ticket ID: {} for reservation ID: {}",
                finalTicket.getId(), reservationDTO.getId());

        try {
            producer.send(mapToTicketDTO(finalTicket));
            LOG.info("Sent ticket ID {} to notification queue.", finalTicket.getId());
        } catch (Exception e) {
            LOG.error("Failed to send ticket ID {} to Kafka notification queue: {}", finalTicket.getId(), e.getMessage(), e);
            throw new TicketGenerationException("Failed to send ticket to notification queue for ticket ID " + finalTicket.getId(), e);
        }
        return finalTicket;
    }

    @Transactional
    public TicketDTO validateTicket(Long ticketId) {
        LOG.info("Attempting to validate ticket with ID: {}", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " not found for validation."));

        if (ticket.getValidatedAt() != null) {
            LOG.warn("Ticket ID: {} has already been validated at {}.", ticketId, ticket.getValidatedAt());
            throw new TicketValidationException("Ticket with ID " + ticketId + " was already validated at " + ticket.getValidatedAt());
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            LOG.warn("Ticket ID: {} has already been used (status: {}). Validated at {}", ticketId, ticket.getStatus(), ticket.getValidatedAt());
            throw new TicketValidationException("Ticket with ID " + ticketId + " was already used at " + ticket.getValidatedAt());
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            LOG.warn("Attempt to validate a CANCELLED ticket ID: {}.", ticketId);
            throw new TicketValidationException("Ticket with ID " + ticketId + " is CANCELLED and cannot be validated.");
        }

        ticket.setValidatedAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.USED);
        try {
            Ticket validatedTicket = ticketRepository.save(ticket);
            LOG.info("Ticket ID: {} successfully validated at {}.", validatedTicket.getId(), validatedTicket.getValidatedAt());
            return mapToTicketDTO(validatedTicket);
        } catch (Exception e) {
            LOG.error("Error saving validated ticket ID {}: {}", ticketId, e.getMessage(), e);
            throw new TicketGenerationException("Failed to save validated ticket " + ticketId, e); // Używam ogólnego, można stworzyć bardziej specyficzny
        }
    }

    @Transactional(readOnly = true)
    public byte[] getQrCodeImageForTicket(Long ticketId) throws IOException, WriterException {
        LOG.debug("Requesting QR code image for ticket ID: {}", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " not found for QR code generation."));

        if (ticket.getQrCodeData() == null || ticket.getQrCodeData().isEmpty()) {
            LOG.error("QR code data is missing for ticket ID: {}", ticketId);
            throw new IllegalStateException("QR code data is missing for ticket " + ticketId + ". Cannot generate image.");
        }

        try {
            return qrCodeGeneratorService.generateQrCodeImage(ticket.getQrCodeData(), 500, 500);
        } catch (IOException | WriterException e) {
            LOG.error("Failed to generate QR code image for ticket ID {}: {}", ticketId, e.getMessage(), e);
            throw new TicketGenerationException("Failed to generate QR image for ticket " + ticketId, e);
        }
    }

    @Transactional(readOnly = true)
    public TicketDTO getTicketById(Long ticketId) {
        LOG.debug("Fetching ticket by ID: {}", ticketId);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " not found."));
        return mapToTicketDTO(ticket);
    }

    @Transactional(readOnly = true)
    public TicketDTO getTicketByReservationId(Long reservationId) {
        LOG.debug("Fetching ticket by reservation ID: {}", reservationId);
        Ticket ticket = ticketRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket for reservation ID " + reservationId + " not found."));
        return mapToTicketDTO(ticket);
    }

    @Transactional
    public void handleReservationCancellation(Long reservationId) {
        LOG.info("Handling ticket invalidation for cancelled reservation ID: {}", reservationId);
        Optional<Ticket> ticketOpt = ticketRepository.findByReservationId(reservationId);
        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            if (ticket.getStatus() == TicketStatus.USED) {
                LOG.warn("Reservation ID: {} was cancelled, but its ticket ID: {} was already USED at {}. This is an unusual scenario requiring review.",
                        reservationId, ticket.getId(), ticket.getValidatedAt());
            } else if (ticket.getStatus() != TicketStatus.CANCELLED) {
                ticket.setStatus(TicketStatus.CANCELLED);
                ticketRepository.save(ticket);
                LOG.info("Ticket ID: {} (for reservation ID: {}) marked as CANCELLED.", ticket.getId(), reservationId);
            } else {
                LOG.info("Ticket ID: {} (for reservation ID: {}) was already CANCELLED.", ticket.getId(), reservationId);
            }
        } else {
            LOG.info("No ticket found for reservation ID {} during cancellation handling. No action for ticket.", reservationId);
        }
    }

    private TicketDTO mapToTicketDTO(Ticket ticket) {
        return TicketDTO.builder()
                .id(ticket.getId())
                .reservationId(ticket.getReservationId())
                .screeningId(ticket.getScreeningId())
                .movieTitle(ticket.getMovieTitle())
                .screeningStartTime(ticket.getScreeningStartTime())
                .hallNumber(ticket.getHallNumber())
                .customerName(ticket.getCustomerName())
                .customerEmail(ticket.getCustomerEmail())
                .totalPrice(ticket.getPrice())
                .seatsDescription(ticket.getSeatsDescription())
                .qrCodeData(ticket.getQrCodeData())
                .createdAt(ticket.getCreatedAt())
                .validatedAt(ticket.getValidatedAt())
                .status(ticket.getStatus())
                .build();
    }
}
