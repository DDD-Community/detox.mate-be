package com.detoxmate.auth.dto;

public record KakaoSocialLoginResponse(
        Long id,
        String displayName,
        String profileImageUrl,
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
}
