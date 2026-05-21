package com.detoxmate.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record GroupMemberProfileResponse(
        Long groupMemberId,
        Long userId,
        Long groupId,
        String displayName,
        String profileImageUrl,
        String role,
        String memberStatus,
        LocalDateTime joinedAt,
        String goalStatus,
        @JsonProperty("isUserWithdrawn")
        boolean userWithdrawn,
        List<GroupMemberUsageGoalResponse> currentGoals,
        GroupMemberGoalChangeAvailabilityResponse goalChangeAvailability,
        GroupMemberActivitySummaryResponse activitySummary,
        GroupMemberWeeklySummaryResponse weeklySummary
) {
}
