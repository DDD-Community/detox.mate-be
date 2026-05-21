package com.detoxmate.upload.service;

import com.detoxmate.upload.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageReadUrlBuilder {

    private final StorageProperties storageProperties;

    public String build(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String baseUrl = storageProperties.imageReadBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }

        return trimTrailingSlash(baseUrl) + "/" + trimLeadingSlash(objectKey);
    }

    private String trimTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private String trimLeadingSlash(String value) {
        int start = 0;
        while (start < value.length() && value.charAt(start) == '/') {
            start++;
        }
        return value.substring(start);
    }
}
