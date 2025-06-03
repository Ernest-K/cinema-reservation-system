FROM openjdk:21-jdk-slim

COPY target/*.jar movie-service.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "movie-service.jar"]
