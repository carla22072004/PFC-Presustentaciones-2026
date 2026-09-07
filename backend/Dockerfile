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

# pg_dump / pg_restore para el apartado "Gestión de Respaldos" del administrador
# (BackupService). El cliente de postgres de Alpine es compatible hacia atrás con el
# servidor Postgres 15 de docker-compose.
RUN apk add --no-cache postgresql16-client

# Add a non-root user to run the application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Directorio de archivos generados en tiempo de ejecución (PDFs de actas, respaldos .dump).
# En docker-compose se monta encima un volumen persistente; esto solo garantiza que exista
# y sea escribible cuando se corre la imagen suelta.
RUN mkdir -p /app/uploads/actas /app/uploads/backups && chown -R appuser:appgroup /app/uploads

# Copy the compiled jar from the build stage and assign ownership
COPY --from=build --chown=appuser:appgroup /app/target/presustentaciones-1.0.0-SNAPSHOT.jar app.jar

# Switch to the non-root user
USER appuser

# Expose backend port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
