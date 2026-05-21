package com.detoxmate.auth.dto;

public record AuthLoginResponse(
        Long id,
        String displayName,
        String profileImageUrl,
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
}
