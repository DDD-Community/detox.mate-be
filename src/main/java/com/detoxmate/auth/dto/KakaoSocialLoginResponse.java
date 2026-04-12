package com.detoxmate.auth.dto;

public record KakaoSocialLoginResponse(
        Long id,
        String displayName,
        String profileImageUrl,
        String accessToken,
        long accessTokenExpiresIn,
        boolean isNewUser
) {
}
