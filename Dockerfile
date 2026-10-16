# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the application, completely skipping tests and test dependencies to avoid network glitches
RUN mvn clean package -Dmaven.test.skip=true -Dmaven.wagon.http.retryHandler.count=5 --batch-mode

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Cài đặt Node.js để chạy kèm Zalo Gateway
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y --no-install-recommends nodejs && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy thư mục zalo-gateway và cài đặt thư viện
COPY zalo-gateway ./zalo-gateway
RUN cd zalo-gateway && npm install --omit=dev

# Copy the built jar from the build stage
COPY --from=build /app/target/payroll-0.0.1-SNAPSHOT.jar app.jar

# Expose the port (Render will provide the PORT env var)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
