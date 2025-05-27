package com.example.ticket_service.service;

import com.example.ticket_service.kafka.producer.MessageProducer;
import org.example.commons.dto.*;
import org.example.commons.events.TicketGenerationFailedEvent;
import com.example.ticket_service.entity.Ticket;
import com.example.ticket_service.repository.TicketRepository;
import com.google.zxing.WriterException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        LOG.info("Attempting to generate a single ticket for reservation ID: {}", reservationDTO.getId());

        // Sprawdzenie, czy bilet dla tej rezerwacji już istnieje (dzięki unique constraint na reservationId)
        Optional<Ticket> existingTicketOpt = ticketRepository.findByReservationId(reservationDTO.getId());

        if (existingTicketOpt.isPresent()) {
            LOG.warn("Ticket for reservation ID: {} already exists. Returning existing ticket.", reservationDTO.getId());
            return existingTicketOpt.get();
        }

        if (reservationDTO.getSeats() == null || reservationDTO.getSeats().isEmpty()) {
            LOG.warn("No seats found in reservation DTO for reservation ID: {}. Cannot generate ticket.", reservationDTO.getId());
            producer.sendTicketGenerationFailed(new TicketGenerationFailedEvent(reservationDTO.getId(), "No seats provided"));
            throw new IllegalArgumentException("Cannot generate ticket for reservation " + reservationDTO.getId() + " without seats.");
        }

        ScreeningDTO screening = reservationDTO.getScreeningDTO();

        // Przygotowanie opisu miejsc do zapisu w encji (dla łatwiejszego odczytu)
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
                .build();

        Ticket savedTicketWithId = ticketRepository.saveAndFlush(ticket);

        List<String> seatsInfoListForQr = reservationDTO.getSeats().stream()
                .map(seat -> "Row " + seat.getRowNumber() + ", Seat " + seat.getSeatNumber())
                .collect(Collectors.toList());

        // Przygotowanie danych do kodu QR
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
        String qrCodeText = qrCodeGeneratorService.generateQrCodeText(qrPayload);

        savedTicketWithId.setQrCodeData(qrCodeText);
        Ticket savedTicket = ticketRepository.save(savedTicketWithId);

        LOG.info("Successfully generated and saved a single ticket (UID: {}) for reservation ID: {}",
                savedTicket.getId(), reservationDTO.getId());

        producer.send(mapToTicketDTO(savedTicket));

        return savedTicket;
    }

    @Transactional
    public TicketDTO validateTicket(Long ticketId) {
        LOG.info("Attempting to validate ticket with ID: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    LOG.warn("Ticket not found for validation with ID: {}", ticketId);
                    return new NotFoundException("Ticket with ID " + ticketId + " not found.");
                });

        if (ticket.getValidatedAt() != null) {
            LOG.warn("Ticket with ID: {} has already been validated at {}.", ticketId, ticket.getValidatedAt());
            throw new NotFoundException(
                    "Ticket with ID " + ticketId + " was already validated at " + ticket.getValidatedAt()
            );
        }

        ticket.setValidatedAt(LocalDateTime.now());
        Ticket validatedTicket = ticketRepository.save(ticket);

        LOG.info("Ticket with ID: {} successfully validated at {}.", validatedTicket.getId(), validatedTicket.getValidatedAt());
        return mapToTicketDTO(validatedTicket);
    }

    @Transactional(readOnly = true) // Tylko odczyt z bazy
    public byte[] getQrCodeImageForTicket(Long ticketId) throws IOException, WriterException {
        LOG.debug("Requesting QR code image for ticket ID: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    LOG.warn("Ticket not found when requesting QR code image for ID: {}", ticketId);
                    return new NotFoundException("Ticket with ID " + ticketId + " not found.");
                });

        if (ticket.getQrCodeData() == null || ticket.getQrCodeData().isEmpty()) {
            LOG.error("QR code data is missing for ticket ID: {}", ticketId);
            // Można rzucić inny, specyficzny wyjątek lub zwrócić domyślny obrazek błędu
            throw new IllegalStateException("QR code data is missing for ticket " + ticketId);
        }

        // Generuj obrazek QR używając danych z biletu
        return qrCodeGeneratorService.generateQrCodeImage(ticket.getQrCodeData(), 500, 500);
    }

    public TicketDTO getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket not found"));

        return mapToTicketDTO(ticket);
    }

    public TicketDTO getTicketByReservationId(Long reservationId) {
        Ticket ticket = ticketRepository.findByReservationId(reservationId).orElseThrow(() -> new NotFoundException("Ticket not found"));

        return mapToTicketDTO(ticket);
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
                .build();
    }
}
