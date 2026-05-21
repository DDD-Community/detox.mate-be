package com.detoxmate.user.dto;

public record MyProfileResponse(
        Long id,
        String displayName,
        String profileImageUrl,
        boolean pushNotificationEnabled
) {
}
