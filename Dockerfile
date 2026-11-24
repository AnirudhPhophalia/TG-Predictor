FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /application
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S predictor && adduser -S predictor -G predictor
WORKDIR /application
RUN mkdir data && chown predictor:predictor data
COPY --from=build --chown=predictor:predictor /application/target/thapar-gpf-1.0.0.jar application.jar
USER predictor
# deployed copies always use PostgreSQL (Neon / Render); the host's PORT variable picks the port
ENV SPRING_PROFILES_ACTIVE=postgres
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
