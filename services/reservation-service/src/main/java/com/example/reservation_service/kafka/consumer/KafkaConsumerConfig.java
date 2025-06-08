package com.example.reservation_service.kafka.consumer;

import org.example.commons.dto.PaymentStatusDTO;
import org.example.commons.events.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, PaymentStatusDTO> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.reservation_service.dto");
        return new DefaultKafkaConsumerFactory<>(configProps,
                new StringDeserializer(),
                new JsonDeserializer<>(PaymentStatusDTO.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentStatusDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentStatusDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, TicketGenerationFailedEvent> ticketFailedConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.dto");
        return new DefaultKafkaConsumerFactory<>(configProps,
                new StringDeserializer(),
                new JsonDeserializer<>(TicketGenerationFailedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TicketGenerationFailedEvent> ticketFailedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TicketGenerationFailedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(ticketFailedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, PaymentFailedEvent> paymentFailedConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.dto");
        return new DefaultKafkaConsumerFactory<>(configProps,
                new StringDeserializer(),
                new JsonDeserializer<>(PaymentFailedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentFailedEvent> paymentFailedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentFailedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentFailedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ScreeningCreatedEvent> screeningCreatedEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group-reservation"); // Lub dedykowana grupa np. "reservation-screening-events-group"
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.*,java.util,java.time,java.math"); // Pozwól na dto i eventy z commons
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.example.commons.events.ScreeningCreatedEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(ScreeningCreatedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScreeningCreatedEvent> screeningCreatedEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ScreeningCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(screeningCreatedEventConsumerFactory());
        return factory;
    }

    // Dla ScreeningUpdatedEvent
    @Bean
    public ConsumerFactory<String, ScreeningUpdatedEvent> screeningUpdatedEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group-reservation");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.*,java.util,java.time,java.math");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.example.commons.events.ScreeningUpdatedEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(ScreeningUpdatedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScreeningUpdatedEvent> screeningUpdatedEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ScreeningUpdatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(screeningUpdatedEventConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ScreeningCancelledEvent> screeningCancelledEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group-reservation");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.example.commons.events.ScreeningCancelledEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(ScreeningCancelledEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScreeningCancelledEvent> screeningCancelledEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ScreeningCancelledEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(screeningCancelledEventConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, TicketValidatedEvent> ticektValidatedEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group-reservation");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.example.commons.events.TicketValidatedEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(TicketValidatedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TicketValidatedEvent> ticketValidatedEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TicketValidatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(ticektValidatedEventConsumerFactory());
        return factory;
    }
}
