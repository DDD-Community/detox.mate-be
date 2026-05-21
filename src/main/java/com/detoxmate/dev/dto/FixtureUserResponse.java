package com.detoxmate.dev.dto;

public record FixtureUserResponse(
        String role,
        Long userId,
        String displayName,
        String accessToken
) {
}
