FROM openjdk:21-jdk-slim

COPY target/*.jar payment-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "payment-service.jar"]

# docker build -t pwr260654/cinema-payment-service:latest .
