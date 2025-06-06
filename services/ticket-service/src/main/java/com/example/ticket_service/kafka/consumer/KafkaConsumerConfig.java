package com.example.ticket_service.kafka.consumer;

import org.example.commons.dto.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.commons.events.ReservationCancelledEvent;
import org.example.commons.events.ScreeningUpdatedEvent;
import org.example.commons.events.ScreeningCancelledEvent;
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
    public ConsumerFactory<String, ReservationDTO> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.reservation_service.dto");
        return new DefaultKafkaConsumerFactory<>(configProps,
                new StringDeserializer(),
                new JsonDeserializer<>(ReservationDTO.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservationDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReservationDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ReservationCancelledEvent> reservationCancelledEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.example.commons.events.ReservationCancelledEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(ReservationCancelledEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservationCancelledEvent> reservationCancelledEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReservationCancelledEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reservationCancelledEventConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ScreeningUpdatedEvent> screeningUpdatedEventConsumerFactoryTicket() { // Dodaj suffix np. Ticket
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Dedykowana grupa dla ticket-service, aby oba (reservation i ticket) otrzymały event
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group-ticket");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.*,java.util,java.time,java.math");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.example.commons.events.ScreeningUpdatedEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(ScreeningUpdatedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScreeningUpdatedEvent> screeningUpdatedEventKafkaListenerContainerFactory() { // Ta nazwa była w błędzie
        ConcurrentKafkaListenerContainerFactory<String, ScreeningUpdatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(screeningUpdatedEventConsumerFactoryTicket()); // Użyj poprawnej ConsumerFactory
        // TODO: Rozważ ErrorHandler + DLT
        return factory;
    }

    // Dla ScreeningCancelledEvent (jeśli ticket-service też ma na niego reagować inaczej niż na ReservationCancelledEvent)
    // Jeśli ReservationCancelledEvent wystarcza (bo zawiera reservationId, a ticket ma reservationId),
    // to osobny listener na ScreeningCancelledEvent w TicketService może nie być potrzebny,
    // chyba że chcesz logiki specyficznej dla anulowania *seansu* a nie *rezerwacji*.
    // Obecnie masz już listener na ReservationCancelledEvent, który powinien obsłużyć anulowanie biletu.
    // Poniższy bean jest na wypadek, gdybyś miał dedykowany listener dla ScreeningCancelledEvent w TicketService.

    @Bean
    public ConsumerFactory<String, ScreeningCancelledEvent> screeningCancelledEventConsumerFactoryTicket() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group-ticket");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.example.commons.events.ScreeningCancelledEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(ScreeningCancelledEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScreeningCancelledEvent> screeningCancelledEventKafkaListenerContainerFactory() { // Ta nazwa też była w błędzie
        ConcurrentKafkaListenerContainerFactory<String, ScreeningCancelledEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(screeningCancelledEventConsumerFactoryTicket());
        // TODO: Rozważ ErrorHandler + DLT
        return factory;
    }

}
