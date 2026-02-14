# ---------- BUILD STAGE ----------
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom.xml first (for dependency caching)
COPY pom.xml .

# Download dependencies (this layer will be cached)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B


# ---------- RUN STAGE ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Install curl for healthcheck (wget not available in all alpine images)
RUN apk add --no-cache curl

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring

# Expose Spring Boot default port
EXPOSE 8080

# Health check using actuator endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containers with Java 21
# -XX:+UseContainerSupport: Enables container-aware JVM
# -XX:MaxRAMPercentage: Use 75% of container memory for heap
# -Djdk.tls.client.protocols: Required for MongoDB Atlas & Redis Cloud SSL/TLS connections
# -Dspring.profiles.active: Set active profile from environment variable
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3 \
    -Dfile.encoding=UTF-8"

# Environment variables with defaults (override in docker-compose or deployment)
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
