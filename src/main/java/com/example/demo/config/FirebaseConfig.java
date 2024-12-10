package com.example.demo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() {
        // Retrieve the Firebase credentials from the environment variable
        String firebaseCredentials = System.getenv("FIREBASE_CREDENTIALS");

        if (firebaseCredentials == null || firebaseCredentials.isEmpty()) {
            throw new IllegalStateException("FIREBASE_CREDENTIALS environment variable is not set.");
        }

        try {
            // Use the credentials from the environment variable
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(
                        GoogleCredentials.fromStream(
                            new ByteArrayInputStream(firebaseCredentials.getBytes())
                        )
                    )
                    .build();

            // Initialize FirebaseApp if not already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            } else {
                return FirebaseApp.getInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}
