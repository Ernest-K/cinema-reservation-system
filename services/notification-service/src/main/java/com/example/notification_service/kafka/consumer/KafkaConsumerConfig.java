package com.example.notification_service.kafka.consumer;

import org.example.commons.dto.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.commons.events.ReservationCancelledEvent;
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
    public ConsumerFactory<String, TicketDTO> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.reservation_service.dto");
        return new DefaultKafkaConsumerFactory<>(configProps,
                new StringDeserializer(),
                new JsonDeserializer<>(TicketDTO.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TicketDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TicketDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ReservationCancelledEvent> reservationCancelledEventConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.reservation_service.dto");
        return new DefaultKafkaConsumerFactory<>(configProps,
                new StringDeserializer(),
                new JsonDeserializer<>(ReservationCancelledEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservationCancelledEvent> reservationCancelledEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReservationCancelledEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reservationCancelledEventConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ScreeningChangeNotificationDTO> screeningChangeNotificationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group-notification"); // Ta sama grupa co dla TicketDTO, bo to ten sam serwis
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.*,java.util,java.time,java.math");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.example.commons.dto.ScreeningChangeNotificationDTO");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(ScreeningChangeNotificationDTO.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScreeningChangeNotificationDTO> screeningChangeNotificationKafkaListenerContainerFactory(
            ConsumerFactory<String, ScreeningChangeNotificationDTO> screeningChangeNotificationConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ScreeningChangeNotificationDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(screeningChangeNotificationConsumerFactory);

        return factory;
    }
}
