package com.detoxmate.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record DevTestLoginRequest(
        @NotBlank(message = "testUserKey is required")
        String testUserKey
) {
}
