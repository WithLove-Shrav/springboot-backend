# Use an OpenJDK base image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy Maven wrapper and project files
COPY . .

# Build the project
RUN ./mvnw clean package

# Copy the built JAR file into the container
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

# Expose the default Spring Boot port (8080)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
