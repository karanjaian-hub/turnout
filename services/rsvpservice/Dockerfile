FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy parent POM
COPY pom.xml .

# Copy ALL module POMs so Maven can read the full reactor without errors
# Only POMs here — src is copied later to keep this layer cached
COPY shared/common-dto/pom.xml shared/common-dto/pom.xml
COPY shared/common-exceptions/pom.xml shared/common-exceptions/pom.xml
COPY services/authservice/pom.xml services/authservice/pom.xml
COPY services/eventservice/pom.xml services/eventservice/pom.xml
COPY services/guestservice/pom.xml services/guestservice/pom.xml
COPY services/emailservice/pom.xml services/emailservice/pom.xml
COPY services/rsvpservice/pom.xml services/rsvpservice/pom.xml
COPY services/notificationservice/pom.xml services/notificationservice/pom.xml
COPY services/paymentservice/pom.xml services/paymentservice/pom.xml
COPY services/aiservice/pom.xml services/aiservice/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml

# Copy shared module sources
COPY shared/common-dto/src shared/common-dto/src
COPY shared/common-exceptions/src shared/common-exceptions/src

# Copy only the target service source
ARG SERVICE_PATH=services/rsvpservice
COPY ${SERVICE_PATH}/src ${SERVICE_PATH}/src

# Install shared modules then build the target service
RUN mvn install -pl shared/common-dto,shared/common-exceptions -am -q && \
    mvn package -pl ${SERVICE_PATH} -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S turnout && adduser -S turnout -G turnout
USER turnout

ARG SERVICE_PATH=services/rsvpservice
COPY --from=builder /app/${SERVICE_PATH}/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q -O- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Xmx200m", "-Dspring.threads.virtual.enabled=true", "-jar", "app.jar"]
