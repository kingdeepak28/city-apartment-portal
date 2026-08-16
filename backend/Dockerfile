# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build

# Cache dependencies separately from source so `docker build` skips the download step
# on source-only changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /build/target/backend.jar app.jar
RUN mkdir -p /data/uploads && chown -R app:app /app /data/uploads

USER app
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# MaxRAMPercentage (not a fixed -Xmx) lets the JVM size its heap to whatever memory limit the
# container is actually given (see docker-compose.prod.yml's `deploy.resources.limits.memory`),
# instead of guessing a number that's wrong on a different-sized host.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
