package com.example.ticket_service;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@OpenAPIDefinition(
    servers = {
       @Server(url = "/", description = "Default Server URL")
    }
)
@EnableDiscoveryClient
@SpringBootApplication
public class TicketServiceApplication {

    public static void main(String[] args) {
		SpringApplication.run(TicketServiceApplication.class, args);
	}
}
