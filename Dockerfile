# Use an OpenJDK image as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file from the target directory into the container
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

# Expose the default Spring Boot port (8080)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]

