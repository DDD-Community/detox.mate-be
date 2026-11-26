package com.detoxmate.upload.config;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Profile("prod")
@EnableConfigurationProperties({StorageProperties.class, StorageGcsProperties.class})
public class GcsPhotoConfig {

    @Bean
    Storage photoStorage(@Value("${firebase.service-account-path}") Resource account) throws IOException {
        try (InputStream input = account.getInputStream()) {
            ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(input);
            return StorageOptions.newBuilder()
                    .setProjectId(credentials.getProjectId())
                    .setCredentials(credentials)
                    .build().getService();
        }
    }
}
