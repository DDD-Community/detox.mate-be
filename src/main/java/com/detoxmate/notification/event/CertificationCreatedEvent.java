package com.detoxmate.notification.event;

public record CertificationCreatedEvent(
        Long challengeRecordId,
        Long actorUserId
) {
}
