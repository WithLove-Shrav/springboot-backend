package com.example.demo.config;

import com.google.firebase.FirebaseApp;

import com.google.firebase.FirebaseOptions;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            // If FirebaseApp is not already initialized, initialize it
            FileInputStream serviceAccount =
                    new FileInputStream("src/main/resources/firebase_admin_credentials.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);  // Initialize FirebaseApp with the provided credentials
        }
        return FirebaseApp.getInstance();  // If already initialized, return the existing instance
    }
}
