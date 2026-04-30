package com.detoxmate.upload.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage.s3")
public record StorageS3Properties(
        @NotBlank
        String region,

        @NotBlank
        String bucketName,

        @Positive
        int presignedUrlExpiresIn
) {
}
