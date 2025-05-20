FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/notification-service-0.0.1-SNAPSHOT.jar notification-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "notification-service.jar"]
