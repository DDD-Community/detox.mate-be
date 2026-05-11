package com.detoxmate.feed.dto.response;

import java.time.LocalDateTime;

public record GroupChallengeOverviewResponse(
        Long groupChallengeId,
        Long groupId,
        String groupName,
        int challengeNo,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int streakCount
) {
}
