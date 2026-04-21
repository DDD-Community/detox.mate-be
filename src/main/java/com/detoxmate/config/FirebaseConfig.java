package com.detoxmate.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path:}")
    private Resource serviceAccountResource;

    @PostConstruct
    public void init() throws IOException{
        // detoxmate-dev-firebase-account.json 읽기
        if(!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        if (serviceAccountResource == null || !serviceAccountResource.exists()) {
            log.warn("Firebase service account file not found. Firebase initialization skipped.");
            return;
        }

        try(InputStream inputStream = serviceAccountResource.getInputStream()){
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully.");
        }catch(Exception e){
            log.warn("Firebase initialization failed. Firebase features will be disabled.", e);
        }
    }
}
