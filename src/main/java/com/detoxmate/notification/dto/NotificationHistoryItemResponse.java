package com.detoxmate.notification.dto;

import java.time.LocalDateTime;

public record NotificationHistoryItemResponse(
        Long id,
        String title,
        String message,
        Long senderUserId,
        String senderProfileImageUrl,
        boolean read,
        String targetType,
        Long targetId,
        String sourceType,
        Long sourceId,
        LocalDateTime createdAt
) {
}
