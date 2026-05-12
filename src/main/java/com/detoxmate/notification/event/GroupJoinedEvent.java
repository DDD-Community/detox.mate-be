package com.detoxmate.notification.event;

public record GroupJoinedEvent(
        Long groupId,
        Long joinedUserId
) {
}
