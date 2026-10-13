package com.detoxmate.notification.event;

public record TimeLimitChangedEvent(Long userId, int totalLockMinutes) {
}
