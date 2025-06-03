FROM openjdk:21-jdk-slim

COPY target/*.jar payment-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "payment-service.jar"]
