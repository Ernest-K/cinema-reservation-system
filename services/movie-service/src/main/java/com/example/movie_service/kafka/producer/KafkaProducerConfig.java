package com.example.movie_service.kafka.producer;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.commons.events.ScreeningCancelledEvent;
import org.example.commons.events.ScreeningCreatedEvent;
import org.example.commons.events.ScreeningUpdatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private <T> ProducerFactory<String, T> createProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Aby uniknąć problemów z typami, można dodać nagłówki __TypeId__
        // configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // Domyślnie true dla nowych wersji
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, ScreeningCreatedEvent> screeningCreatedKafkaTemplate() {
        return new KafkaTemplate<>(createProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, ScreeningUpdatedEvent> screeningUpdatedKafkaTemplate() {
        return new KafkaTemplate<>(createProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, ScreeningCancelledEvent> screeningCancelledKafkaTemplate() {
        return new KafkaTemplate<>(createProducerFactory());
    }
}