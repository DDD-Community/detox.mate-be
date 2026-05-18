package com.detoxmate.notification.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.notification.dto.NotificationHistoryListResponse;
import com.detoxmate.notification.dto.NotificationNavigationResponse;
import com.detoxmate.notification.service.NotificationHistoryService;
import com.detoxmate.notification.service.NotificationNavigationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationHistoryController {

    private final NotificationHistoryService notificationHistoryService;
    private final NotificationNavigationService notificationNavigationService;

    @GetMapping("/notifications")
    public NotificationHistoryListResponse getMyNotifications(CurrentUser currentUser) {
        return notificationHistoryService.getMyNotifications(currentUser.id());
    }

    @GetMapping("/notifications/{notificationHistoryId}/navigation")
    public NotificationNavigationResponse getNotificationHistory(CurrentUser currentUser,
                                                                 @PathVariable("notificationHistoryId") Long notificationHistoryId) {

        return notificationNavigationService.resolve(currentUser.id(),notificationHistoryId);
    }
}
