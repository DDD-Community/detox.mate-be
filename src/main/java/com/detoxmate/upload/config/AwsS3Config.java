package com.detoxmate.upload.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Profile("!prod")
@EnableConfigurationProperties({StorageProperties.class, StorageS3Properties.class})
public class AwsS3Config {

    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner(StorageS3Properties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .build();
    }
}
