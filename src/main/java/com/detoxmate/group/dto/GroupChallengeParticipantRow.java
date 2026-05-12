package com.detoxmate.group.dto;

import java.time.LocalDateTime;

public record GroupChallengeParticipantRow(
        Long groupChallengeId,
        Long id,
        Long groupMemberId,
        Long userId,
        String displayName,
        String profileImageObjectKey,
        String status,
        LocalDateTime joinedAt,
        LocalDateTime withdrawnAt,
        boolean userWithdrawn
) {
}
