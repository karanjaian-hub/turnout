# =============================================================================
# Stage 1 — Build
# Maven compiles the code and packages it into a JAR
# We use a separate build stage so the final image doesn't carry Maven,
# source code, or any build tools — keeps it small and secure
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy parent POM and shared modules first — Docker caches these layers
# so a code change in one service doesn't re-download all dependencies
COPY pom.xml .
COPY shared/common-dto/pom.xml shared/common-dto/pom.xml
COPY shared/common-exceptions/pom.xml shared/common-exceptions/pom.xml

# Copy the service POM (build context is the service folder)
COPY pom.xml service-pom.xml

# Download dependencies without building — cached unless POM changes
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Now copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 2 — Runtime
# Starts fresh from a minimal JRE image — no Maven, no source, no build cache
# Final image is ~200MB instead of ~600MB
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user — running as root inside a container is a security risk
RUN addgroup -S turnout && adduser -S turnout -G turnout
USER turnout

# Copy only the built JAR from Stage 1
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Health check — Docker and docker-compose use this to know when the service
# is actually ready (not just started)
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q -O- http://localhost:8080/actuator/health || exit 1

# Virtual threads flag — this is what lets Spring Boot handle thousands of
# concurrent requests without a reactive (WebFlux) programming model
ENTRYPOINT ["java", "-Dspring.threads.virtual.enabled=true", "-jar", "app.jar"]
