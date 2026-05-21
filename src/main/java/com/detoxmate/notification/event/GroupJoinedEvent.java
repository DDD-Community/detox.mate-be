package com.detoxmate.notification.event;

public record GroupJoinedEvent(
        Long groupId,
        Long groupChallengeId,
        Long joinedUserId
) {
}
