package com.detoxmate.group.dto;

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
        boolean isWithdrawn
) {
}
