package com.example.payment_service.kafka.consumer;

import org.example.commons.dto.ReservationDTO;
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
    public ConsumerFactory<String, ReservationCancelledEvent> cancelConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "cinema-group-payment");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.commons.*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.example.commons.events.ReservationCancelledEvent");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(configProps,
                new StringDeserializer(),
                new JsonDeserializer<>(ReservationCancelledEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservationCancelledEvent> cancelKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReservationCancelledEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cancelConsumerFactory());
        return factory;
    }
}
