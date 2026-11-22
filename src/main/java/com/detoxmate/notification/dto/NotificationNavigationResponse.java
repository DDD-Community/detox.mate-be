package com.detoxmate.notification.dto;

public record NotificationNavigationResponse(
        boolean navigable,
        String targetType,
        Long targetId,
        String fallbackTargetType,
        Long fallbackTargetId,
        String reason,
        String url
) {

    public static NotificationNavigationResponse navigable(String targetType,Long targetId) {
        return new NotificationNavigationResponse(true, targetType, targetId, null, null, null, null);
    }

    public static NotificationNavigationResponse friendPage(String targetType, Long targetId, String url) {
        return new NotificationNavigationResponse(true, targetType, targetId, null, null, null, url);
    }

    public static NotificationNavigationResponse noNavigatable(String reason) {
        return new NotificationNavigationResponse(false,null,null,null,null,reason,null);
    }

    public static NotificationNavigationResponse fallback(String fallbackTargetType, Long fallbackTargetId, String reason) {
        return new NotificationNavigationResponse(false,null,null,fallbackTargetType,fallbackTargetId,reason,null);
    }
}
