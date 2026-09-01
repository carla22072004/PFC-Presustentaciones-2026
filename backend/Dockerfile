# Stage 1: Build stage using Maven and JDK 17
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build jar
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime stage using JRE 17
FROM eclipse-temurin:17-jre-alpine

# Add a non-root user to run the application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the compiled jar from the build stage and assign ownership
COPY --from=build --chown=appuser:appgroup /app/target/presustentaciones-1.0.0-SNAPSHOT.jar app.jar

# Directorio de subidas (anteproyectos, actas, tutorias) escribible por el usuario no-root:
# WORKDIR /app pertenece a root, asi que sin esto Files.createDirectories("uploads/...")
# falla con "permission denied" ("No se pudo crear el directorio de uploads"). En Compose
# se monta encima un named volume (uploads_data) para que los archivos sobrevivan a
# reconstrucciones del contenedor.
RUN mkdir -p /app/uploads/anteproyectos /app/uploads/actas /app/uploads/tutorias \
    && chown -R appuser:appgroup /app/uploads

# Switch to the non-root user
USER appuser

# Expose backend port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
