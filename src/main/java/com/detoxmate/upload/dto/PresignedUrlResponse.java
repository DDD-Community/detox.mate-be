package com.detoxmate.upload.dto;

public record PresignedUrlResponse(
        String uploadUrl,
        String objectKey,
        int expiresInSeconds
) {
}
