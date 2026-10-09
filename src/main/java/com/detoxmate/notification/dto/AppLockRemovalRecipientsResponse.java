package com.detoxmate.notification.dto;

import java.util.List;

public record AppLockRemovalRecipientsResponse(List<Recipient> recipients) {
    public record Recipient(Long userId, String displayName) {
    }
}
