package com.detoxmate.upload.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage.gcs")
public record StorageGcsProperties(
        @NotBlank String bucketName,
        @Positive @Max(604800) int presignedUrlExpiresIn
) {
}
