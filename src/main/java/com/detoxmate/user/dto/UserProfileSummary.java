package com.detoxmate.user.dto;

public record UserProfileSummary(
        Long id,
        String displayName,
        String profileImageUrl,
        boolean isWithdrawn
) {
}
