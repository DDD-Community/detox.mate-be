package com.detoxmate.notification.event;

public record PokeGoalSettingReminderEvent(
        Long challengeRecordId,
        Long senderUserId,
        Long receiverUserId
) {
}
