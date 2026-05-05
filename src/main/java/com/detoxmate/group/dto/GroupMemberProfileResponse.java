package com.detoxmate.group.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GroupMemberProfileResponse(
        Long id,
        Long userId,
        Long groupId,
        String displayName,
        String profileImageUrl,
        String role,
        String status,
        LocalDateTime joinedAt,
        int dayCount,
        List<GroupMemberUsageGoalResponse> currentGoals,
        MemberStatsResponse stats
) {
}
