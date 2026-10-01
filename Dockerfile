# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Cache dependencies layer to reuse when source code changes
COPY pom.xml .
RUN mvn dependency:go-offline 

COPY src ./src
# Skip tests during image build
RUN mvn package -DskipTests 

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Run the application as a non-root user for better security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
