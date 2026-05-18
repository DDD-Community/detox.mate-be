package com.detoxmate.notification.dto;

public record GoalSettingReminderTarget(
        Long groupId,
        Long groupChallengeId,
        Long userId
) {
}
