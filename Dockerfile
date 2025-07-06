# TTB Spark Food Delivery - Order Service Dockerfile
# Multi-stage build for optimization

# Stage 1: Build stage
FROM gradle:8.5-jdk21 AS builder

LABEL maintainer="TTB Spark Development Team"
LABEL version="1.0.0"
LABEL description="Order Service for TTB Spark Food Delivery Platform"

# Set working directory
WORKDIR /app

# Copy gradle files first for better layer caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (this layer will be cached if dependencies don't change)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the application
RUN gradle clean build -x test --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre

# Install basic utilities and create app user for security
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r appuser && \
    useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy application configuration
COPY --from=builder /app/src/main/resources/application.yml application.yml

# Change ownership to app user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM configuration for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Application configuration
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8080

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Optional: Add labels for better organization
LABEL org.opencontainers.image.title="TTB Spark Order Service"
LABEL org.opencontainers.image.description="Microservice for handling food delivery orders"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="TTB Spark"
LABEL org.opencontainers.image.authors="TTB Spark Development Team"
