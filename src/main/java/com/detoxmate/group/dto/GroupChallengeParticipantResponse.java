package com.detoxmate.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record GroupChallengeParticipantResponse(
        Long id,
        Long groupMemberId,
        Long userId,
        String displayName,
        String profileImageUrl,
        String status,
        LocalDateTime joinedAt,
        LocalDateTime withdrawnAt,
        List<GoalTimeResponse> goalTimes,
        @JsonProperty("isUserWithdrawn")
        boolean userWithdrawn
) {
}
