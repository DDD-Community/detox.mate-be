package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.NotificationContext;
import com.detoxmate.notification.domain.NotificationPayload;
import com.detoxmate.notification.domain.NotificationTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUnlockNotificationService {

    private final NotificationService notificationService;

    public void request(Long userId) {
        notificationService.send(NotificationCommand.pushOnly(
                userId,
                NotificationTypeCode.APP_UNLOCK_REQUESTED,
                NotificationContext.empty(),
                NotificationPayload.appUnlockDurationSetting()
        ));
    }
}
