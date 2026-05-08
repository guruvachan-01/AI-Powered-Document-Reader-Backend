# ===== Stage 1: Build =====
FROM maven:3.9.7-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -q

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV TZ=Asia/Kolkata

RUN addgroup -S docqa && adduser -S docqa -G docqa

RUN mkdir -p /app/uploads && chown -R docqa:docqa /app/uploads

RUN apk add --no-cache curl

USER docqa

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-XX:+UseContainerSupport","-XX:+UseG1GC","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]