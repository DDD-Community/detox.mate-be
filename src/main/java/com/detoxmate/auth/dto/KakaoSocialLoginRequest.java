package com.detoxmate.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoSocialLoginRequest(
        @NotBlank(message = "providerAccessToken is required")
        String providerAccessToken
) {
}
