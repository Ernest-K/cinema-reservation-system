FROM openjdk:21-jdk-slim

COPY target/*.jar notification-service.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "notification-service.jar"]
