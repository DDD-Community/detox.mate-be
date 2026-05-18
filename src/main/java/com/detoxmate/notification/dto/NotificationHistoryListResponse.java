package com.detoxmate.notification.dto;

import java.util.List;

public record NotificationHistoryListResponse(
        long unreadCount,
        List<NotificationHistoryGroupResponse> groups
) {
}
