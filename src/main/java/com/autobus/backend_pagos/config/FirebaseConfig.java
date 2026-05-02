package com.autobus.backend_pagos.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.database.url}")
    private String databaseUrl;

    // 1. Añadimos esta nueva variable para leer la ruta del archivo
    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initFirebase() {
        try {
            // 2. Usamos FileInputStream con la ruta externa en lugar de ClassPathResource
            InputStream serviceAccount = new FileInputStream(firebaseConfigPath);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .build();

            // Evitamos inicializarlo dos veces si Spring reinicia el contexto
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("🔥 ¡Conexión con Firebase Realtime Database establecida con éxito!");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al inicializar Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
}