package com.detoxmate.notification.event;

public record PokeCreatedEvent(
        Long challengeRecordId,
        Long senderUserId,
        Long receiverUserId
) {
}
