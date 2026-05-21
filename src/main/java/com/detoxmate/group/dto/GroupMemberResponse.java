package com.detoxmate.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record GroupMemberResponse(
        Long id,
        Long userId,
        String displayName,
        String profileImageUrl,
        String role,
        String status,
        LocalDateTime joinedAt,
        LocalDateTime leftAt,
        @JsonProperty("isUserWithdrawn")
        boolean userWithdrawn
) {
}
