FROM openjdk:21-jdk-slim

COPY target/*.jar ticket-service.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "ticket-service.jar"]
