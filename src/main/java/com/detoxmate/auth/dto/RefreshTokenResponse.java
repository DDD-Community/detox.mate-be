package com.detoxmate.auth.dto;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken
) {
}
