package com.detoxmate.notification.dto;

import java.util.List;

public record NotificationHistoryGroupResponse(
        String label,
        List<NotificationHistoryItemResponse> notifications
) {
}
