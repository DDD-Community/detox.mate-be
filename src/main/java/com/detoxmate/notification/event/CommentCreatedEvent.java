package com.detoxmate.notification.event;

public record CommentCreatedEvent(
        Long challengeRecordId,
        Long commenterUserId,
        Long commentId
) {
}
