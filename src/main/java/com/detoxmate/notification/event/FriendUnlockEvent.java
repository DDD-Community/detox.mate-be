package com.detoxmate.notification.event;

import java.util.List;

public record FriendUnlockEvent(Long unlockUserId, List<Long> recipientUserIds) {
    public FriendUnlockEvent {
        recipientUserIds = List.copyOf(recipientUserIds);
    }
}
