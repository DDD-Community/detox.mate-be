package com.detoxmate.upload.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({StorageProperties.class, StorageS3Properties.class})
public class AwsS3Config {

    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner(StorageS3Properties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .build();
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
