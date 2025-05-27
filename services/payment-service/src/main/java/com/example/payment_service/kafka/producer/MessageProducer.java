package com.example.payment_service.kafka.producer;

import org.example.commons.dto.PaymentStatusDTO;
import org.example.commons.events.PaymentFailedEvent;
import org.example.commons.events.ReservationCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);
    private static final String TOPIC = "cinema.reservation";
    private static final String PAYMENT_STATUS_TOPIC = "cinema.payment.status";
    private static final String PAYMENT_FAILED_TOPIC = "cinema.failed.payment";
    private final KafkaTemplate<String, PaymentStatusDTO> kafkaTemplate;
    private final KafkaTemplate<String, PaymentFailedEvent> failedKafkaTemplate;

    public MessageProducer(KafkaTemplate<String, PaymentStatusDTO> kafkaTemplate, KafkaTemplate<String, PaymentFailedEvent> failedKafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.failedKafkaTemplate = failedKafkaTemplate;
    }

    public void send(PaymentStatusDTO paymentStatusDTO) {
        kafkaTemplate.send(PAYMENT_STATUS_TOPIC, paymentStatusDTO);
        LOG.info("Sent payment status: {} to topic: {}", paymentStatusDTO.getPaymentId(), PAYMENT_STATUS_TOPIC);
    }

    public void sendPaymentFailed(PaymentFailedEvent event) {
        failedKafkaTemplate.send(PAYMENT_FAILED_TOPIC, event);
        LOG.info("Sent failed payment: {} to topic: {}", event.getReservationId(), PAYMENT_FAILED_TOPIC);
    }
}
