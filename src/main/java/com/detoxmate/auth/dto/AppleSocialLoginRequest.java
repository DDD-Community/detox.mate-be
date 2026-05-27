package com.detoxmate.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleSocialLoginRequest(
        @NotBlank(message = "identityToken is required")
        String identityToken,
        @NotBlank(message = "rawNonce is required")
        String rawNonce,
        @NotBlank(message = "authorizationCode is required")
        String authorizationCode,
        String displayName
) {
}
