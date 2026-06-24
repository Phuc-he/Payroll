# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the application, skipping tests to speed up the process
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copy the built jar from the build stage
COPY --from=build /app/target/payroll-0.0.1-SNAPSHOT.jar app.jar
# Expose the port (Render will provide the PORT env var)
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
