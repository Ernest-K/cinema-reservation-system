package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
public class RoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("movie-service", r -> r
                .path("/api/movies/**")
                .uri("lb://movie-service"))
            .route("reservation-service", r -> r
                    .path("/api/reservations/**")
                    .uri("lb://reservation-service"))
            .route("ticket-service", r -> r
                    .path("/api/tickets/**")
                    .uri("lb://ticket-service"))
            .route("payment-service", r -> r
                    .path("/api/payments/**")
                    .uri("lb://payment-service"))
            .route("movie-service-health", r -> r
                .path("/movie-service/actuator/**")
                .filters(f -> f.rewritePath("/movie-service/actuator/(?<segment>.*)", "/actuator/${segment}"))
                .uri("lb://movie-service"))
            .build();
    }
}
