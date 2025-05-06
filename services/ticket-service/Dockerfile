FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/ticket-service-0.0.1-SNAPSHOT.jar ticket-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "ticket-service.jar"]
