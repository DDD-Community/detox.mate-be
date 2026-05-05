package com.detoxmate.group.dto;

import java.time.LocalDateTime;

public record GroupMemberUserQueryResult(
        Long id,
        Long userId,
        String displayName,
        String profileImageObjectKey,
        String role,
        String status,
        LocalDateTime joinedAt,
        LocalDateTime leftAt
) {
}
