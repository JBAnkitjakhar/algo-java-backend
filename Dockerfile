# Use OpenJDK 21 as base image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies (this layer will be cached)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose port 8080
EXPOSE 8080

# Set environment variable for Spring profile
ENV SPRING_PROFILES_ACTIVE=prod

# Run the jar file
CMD ["java", "-jar", "target/algo-arena-0.0.1-SNAPSHOT.jar"]