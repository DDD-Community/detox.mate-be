package com.detoxmate.notification.event;

public record CertificationStartTomorrowEvent(
        Long groupId,
        Long groupChallengeId
) {
}
