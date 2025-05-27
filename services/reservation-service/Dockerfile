FROM openjdk:21-jdk-slim

COPY target/*.jar reservation-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "reservation-service.jar"]

# docker build -t pwr260654/cinema-reservation-service:latest .
