package com.detoxmate.group.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GroupChallengeResponse(
        Long id,
        Long groupId,
        String groupName,
        int challengeNo,
        String status,
        List<GroupChallengeParticipantResponse> participants,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
