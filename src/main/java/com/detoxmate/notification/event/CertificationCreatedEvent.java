package com.detoxmate.notification.event;

public record CertificationCreatedEvent(
        Long recipientUserId,
        String actorName
) {
}
