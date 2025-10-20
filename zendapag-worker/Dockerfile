# Multi-stage build for Zendapag Worker
FROM eclipse-temurin:17-jdk as builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY zendapag-common/pom.xml ./zendapag-common/
COPY zendapag-core/pom.xml ./zendapag-core/
COPY zendapag-worker/pom.xml ./zendapag-worker/

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY zendapag-common/src ./zendapag-common/src
COPY zendapag-core/src ./zendapag-core/src
COPY zendapag-worker/src ./zendapag-worker/src

# Build the application
RUN ./mvnw clean package -pl zendapag-worker -am -DskipTests

# Production stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create app user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the JAR file
COPY --from=builder /app/zendapag-worker/target/zendapag-worker-*.jar app.jar

# Change ownership
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Expose port
EXPOSE 8081

# JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]