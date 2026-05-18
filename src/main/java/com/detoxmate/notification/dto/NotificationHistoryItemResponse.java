package com.detoxmate.notification.dto;

import java.time.LocalDateTime;

public record NotificationHistoryItemResponse(
        Long id,
        String title,
        String message,
        boolean read,
        String targetType,
        Long targetId,
        String sourceType,
        Long sourceId,
        LocalDateTime createdAt
) {
}
