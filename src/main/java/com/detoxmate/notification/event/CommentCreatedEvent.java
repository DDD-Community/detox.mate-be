package com.detoxmate.notification.event;

public record CommentCreatedEvent(
        Long recipientUserId,
        String actorNickname
) {
}
