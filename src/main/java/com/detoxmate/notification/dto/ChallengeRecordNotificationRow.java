package com.detoxmate.notification.dto;

public record ChallengeRecordNotificationRow(
        Long challengeRecordId,
        Long groupChallengeId,
        Long authorUserId,
        String authorNickname
) {
}
