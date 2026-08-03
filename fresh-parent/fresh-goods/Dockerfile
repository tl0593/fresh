FROM eclipse-temurin:17-jdk-alpine
LABEL maintainer="fresh"
WORKDIR /app
COPY target/fresh-goods-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-server", "-Xms256m", "-Xmx512m", "-jar", "app.jar", "--spring.profiles.active=docker"]
