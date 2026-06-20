# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy pom.xml and download dependencies (caches them for faster subsequent builds)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the fat JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create logs directory
RUN mkdir logs

# Copy the built fat JAR from the builder stage
COPY --from=builder /build/target/evolveumCLIapp-1.0-SNAPSHOT.jar ./evolveumCLIapp.jar

# Define the entrypoint so arguments can be passed directly
ENTRYPOINT ["java", "-jar", "evolveumCLIapp.jar"]