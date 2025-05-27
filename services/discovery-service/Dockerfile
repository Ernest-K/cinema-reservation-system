FROM openjdk:21-jdk-slim

COPY target/*.jar discovery-service.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "discovery-service.jar"]

# docker build -t pwr260654/cinema-discovery-service:latest .
