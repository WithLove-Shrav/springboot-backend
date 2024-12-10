# Use an OpenJDK base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy all project files into the container
COPY . .

# Build the JAR file
RUN ./mvnw clean package || cat target/mvn-build.log

# Copy the built JAR file into the container
COPY target/demo-0.0.1-SNAPSHOT app.jar


# Expose the default Spring Boot port (8080)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]



