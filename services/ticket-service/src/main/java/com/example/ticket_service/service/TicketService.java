package com.example.ticket_service.service;

import com.example.ticket_service.dto.QrCodePayload;
import com.example.ticket_service.dto.ReservationDTO;
import com.example.ticket_service.dto.ScreeningDTO;
import com.example.ticket_service.dto.TicketDTO;
import com.example.ticket_service.entity.Ticket;
import com.example.ticket_service.repository.TicketRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketService.class);

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
            throw new IllegalArgumentException("Cannot generate ticket for reservation " + reservationDTO.getId() + " without seats.");
        }

        ScreeningDTO screening = reservationDTO.getScreeningDTO();
        String ticketUid = UUID.randomUUID().toString();

        // Przygotowanie listy informacji o miejscach dla QR kodu
        List<String> seatsInfoListForQr = reservationDTO.getSeats().stream()
                .map(seat -> "Row " + seat.getRowNumber() + ", Seat " + seat.getSeatNumber())
                .collect(Collectors.toList());

        // Przygotowanie opisu miejsc do zapisu w encji (dla łatwiejszego odczytu)
        String seatsDescriptionAggregated = reservationDTO.getSeats().stream()
                .map(seat -> "R" + seat.getRowNumber() + "M" + seat.getSeatNumber())
                .collect(Collectors.joining(", "));


        // Przygotowanie danych do kodu QR
        QrCodePayload qrPayload = new QrCodePayload(
                reservationDTO.getId(),
                screening.getMovieDTO().getTitle(),
                screening.getStartTime(),
                "Hall " + screening.getHallDTO().getNumber(),
                seatsInfoListForQr, // Przekazujemy listę informacji o miejscach
                reservationDTO.getCustomerName(),
                reservationDTO.getSeats().size() // Liczba miejsc
        );
        String qrCodeText = qrCodeGeneratorService.generateQrCodeText(qrPayload);

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
                .qrCodeData(qrCodeText)
                .seatsDescription(seatsDescriptionAggregated)
                .createdAt(LocalDateTime.now())
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        LOG.info("Successfully generated and saved a single ticket (UID: {}) for reservation ID: {}",
                savedTicket.getId(), reservationDTO.getId());
        return savedTicket;
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