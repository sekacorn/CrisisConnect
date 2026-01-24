# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Metadata labels
LABEL maintainer="CrisisConnect Team"
LABEL version="1.0.0"
LABEL description="CrisisConnect Backend Service"

WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose application port
EXPOSE 8080

# Environment variables for demo mode (can be overridden)
ENV SPRING_PROFILES_ACTIVE=demo
ENV SPRING_DATASOURCE_URL=jdbc:h2:mem:crisisconnect_demo
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
ENV SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
ENV SPRING_H2_CONSOLE_ENABLED=true
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop
ENV ADMIN_BOOTSTRAP_ENABLED=true

# Health check - just verify the port is responsive
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/auth/login || exit 1

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
