package com.detoxmate.notification.dto;

public record StreakWarningTarget(
        Long groupId,
        Long groupChallengeId,
        long participantCount,
        long certifiedCount
) {
}
