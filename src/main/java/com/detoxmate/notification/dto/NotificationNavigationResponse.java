package com.detoxmate.notification.dto;

public record NotificationNavigationResponse(
        boolean navigable,
        String targetType,
        Long targetId,
        String fallbackTargetType,
        Long fallbackTargetId,
        String reason
) {

    public static NotificationNavigationResponse navigable(String targetType,Long targetId) {
        return new NotificationNavigationResponse(true, targetType, targetId, null, null, null);
    }

    public static NotificationNavigationResponse noNavigatable(String reason) {
        return new NotificationNavigationResponse(false,null,null,null,null,reason);
    }

    public static NotificationNavigationResponse fallback(String fallbackTargetType, Long fallbackTargetId, String reason) {
        return new NotificationNavigationResponse(false,null,null,fallbackTargetType,fallbackTargetId,reason);
    }
}
