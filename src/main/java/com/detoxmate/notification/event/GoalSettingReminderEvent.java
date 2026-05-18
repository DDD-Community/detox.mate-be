package com.detoxmate.notification.event;

public record GoalSettingReminderEvent(
        Long groupId,
        Long groupChallengeId,
        Long targetUserId
) {
}
