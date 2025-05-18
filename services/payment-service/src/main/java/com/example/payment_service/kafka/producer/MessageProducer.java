package com.example.payment_service.kafka.producer;

import org.example.commons.dto.PaymentStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);
    private static final String TOPIC = "cinema.reservation";
    private static final String PAYMENT_STATUS_TOPIC = "cinema.payment.status";
    private final KafkaTemplate<String, PaymentStatusDTO> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, PaymentStatusDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(PaymentStatusDTO paymentStatusDTO) {
        kafkaTemplate.send(PAYMENT_STATUS_TOPIC, paymentStatusDTO);
        LOG.info("Sent payment status: {} to topic: {}", paymentStatusDTO.getPaymentId(), PAYMENT_STATUS_TOPIC);
    }
}
