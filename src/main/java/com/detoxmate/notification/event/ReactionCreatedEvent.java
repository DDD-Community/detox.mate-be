package com.detoxmate.notification.event;

public record ReactionCreatedEvent(
        Long challengeRecordId,
        Long reactorUserId
) {
}
