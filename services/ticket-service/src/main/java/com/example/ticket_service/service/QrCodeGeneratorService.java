package com.example.ticket_service.service;

import org.example.commons.dto.QrCodePayloadDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeGeneratorService {

    private static final Logger LOG = LoggerFactory.getLogger(QrCodeGeneratorService.class);
    private final ObjectMapper objectMapper;

    public QrCodeGeneratorService() {
        this.objectMapper = new ObjectMapper();
        // Ważne dla poprawnej serializacji LocalDateTime do JSON
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Generuje tekstową reprezentację (JSON) danych dla kodu QR.
     */
    public String generateQrCodeText(QrCodePayloadDTO payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing QR code payload to JSON: {}", payload, e);
            // W praktyce można rzucić własny wyjątek
            throw new RuntimeException("Could not generate QR code text", e);
        }
    }

    /**
     * Opcjonalnie: Generuje obraz QR kodu jako tablicę bajtów (PNG).
     * Ta metoda może nie być potrzebna, jeśli przechowujemy tylko tekst QR w bazie.
     */
    public byte[] generateQrCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // Poziom korekcji błędów

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}