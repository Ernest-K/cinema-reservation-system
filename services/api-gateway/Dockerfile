FROM openjdk:21-jdk-slim

COPY target/*.jar api-gateway.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "api-gateway.jar"]

# docker build -t pwr260654/cinema-api-gateway:latest .
