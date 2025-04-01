# Używamy obrazu JDK 21
FROM eclipse-temurin:21-jdk

# Ustawiamy katalog roboczy
WORKDIR /app

# Kopiujemy skompilowany plik JAR
COPY target/discovery-service-0.0.1-SNAPSHOT.jar discovery-service.jar

# Otwieramy port 8761 dla Eureka Server
EXPOSE 8761

# Uruchamiamy Eureka Server
ENTRYPOINT ["java", "-jar", "discovery-service.jar"]
