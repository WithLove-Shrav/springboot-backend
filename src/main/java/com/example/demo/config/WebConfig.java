package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Apply CORS to all endpoints
                .allowedOrigins("http://192.168.x.x", "http://localhost:3000")  // Specify the allowed origins (e.g., your mobile device IP, local development URLs)
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // Specify the allowed methods
                .allowedHeaders("*");  // Allow all headers
    }
}
