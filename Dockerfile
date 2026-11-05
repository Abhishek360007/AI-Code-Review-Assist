# Single-image build: the React app is bundled into the Spring Boot jar and served from the same
# origin as the API. Used for one-service hosting (e.g. Render). For local development use
# docker-compose.yml, which runs web and server as separate containers.

# --- web ---
FROM node:20-alpine AS web
WORKDIR /web
COPY web/package.json web/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY web/ ./
RUN npm run build

# --- server ---
FROM maven:3.9-eclipse-temurin-17 AS server
WORKDIR /src
COPY server/pom.xml .
RUN mvn -B -q dependency:go-offline
COPY server/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -B -q package -DskipTests

# --- run ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=server /src/target/review-assist-server-*.jar app.jar
USER app
EXPOSE 8080
# Keep memory modest so the app fits small (512 MB) hosting plans.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-Xss512k", "-jar", "/app/app.jar"]
