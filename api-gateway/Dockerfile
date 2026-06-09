FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Parent POM
COPY pom.xml .

# Shared modules — full source (api-gateway depends on these)
COPY shared/common-exceptions/pom.xml shared/common-exceptions/pom.xml
COPY shared/common-exceptions/src     shared/common-exceptions/src
COPY shared/common-dto/pom.xml        shared/common-dto/pom.xml
COPY shared/common-dto/src            shared/common-dto/src

# Other service POMs only — reactor needs them to exist, but we don't build them
COPY services/authservice/pom.xml         services/authservice/pom.xml
COPY services/eventservice/pom.xml        services/eventservice/pom.xml
COPY services/guestservice/pom.xml        services/guestservice/pom.xml
COPY services/emailservice/pom.xml        services/emailservice/pom.xml
COPY services/rsvpservice/pom.xml         services/rsvpservice/pom.xml
COPY services/notificationservice/pom.xml services/notificationservice/pom.xml
COPY services/paymentservice/pom.xml      services/paymentservice/pom.xml
COPY services/aiservice/pom.xml           services/aiservice/pom.xml

# Gateway — full source
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src     api-gateway/src

# -pl api-gateway -am = build api-gateway + its module deps only (common-dto, common-exceptions)
# Other services are in the reactor graph but skipped because they're not in the dep tree
RUN mvn clean package -DskipTests -B -pl api-gateway -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S turnout && adduser -S turnout -G turnout
USER turnout
COPY --from=builder /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q -O- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-Dspring.threads.virtual.enabled=true", "-jar", "app.jar"]
