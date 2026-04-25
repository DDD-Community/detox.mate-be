package com.detoxmate.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@Configuration
@Profile("!test")
public class FirebaseConfig {

    private static final Set<String> FAIL_FAST_PROFILES = Set.of("dev", "prod");

    @Value("${firebase.service-account-path:}")
    private Resource serviceAccountResource;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @PostConstruct
    public void init() {
        // 이미 초기화된 경우 중복 방지
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        // 파일 자체가 없을 때
        if (serviceAccountResource == null || !serviceAccountResource.exists()) {
            handleInitFailure("Firebase service account file not found at configured path", null);
            return;
        }
        // 실제 초기화 시도
        try (InputStream inputStream = serviceAccountResource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully. (profile={})", activeProfile);
        } catch (IOException e) {
            handleInitFailure("Firebase initialization failed", e);
        }
    }

    private void handleInitFailure(String message, Throwable cause) {
        if (FAIL_FAST_PROFILES.contains(activeProfile)) {
            // dev나 prod에서 문제가 생길경우 앱을 띄우지 않음
            throw new IllegalStateException(message + " (profile=" + activeProfile + ")", cause);
        }
        // 기타 환경에서는 로그만 찍기
        log.warn("{} — push features disabled (profile={})", message, activeProfile, cause);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
