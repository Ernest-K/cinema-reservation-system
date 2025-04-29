FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/payment-service-0.0.1-SNAPSHOT.jar payment-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "payment-service.jar"]
