package com.detoxmate.group.dto;

import java.time.LocalDateTime;

public record GroupActivityParticipantRow(
        Long groupChallengeId,
        Long groupChallengeParticipantId,
        Long groupMemberId,
        Long userId,
        String displayName,
        String profileImageObjectKey,
        String memberStatus,
        LocalDateTime memberJoinedAt,
        LocalDateTime memberLeftAt,
        String participantStatus,
        LocalDateTime participantJoinedAt,
        LocalDateTime participantWithdrawnAt
) {
}
