package com.detoxmate.notification.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.notification.dto.AppLockRemovalRecipientsResponse;
import com.detoxmate.notification.dto.AppLockRemovalRequest;
import com.detoxmate.notification.dto.AppUnlockReportRequest;
import com.detoxmate.notification.service.AppLockNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class AppLockNotificationController {

    private final AppLockNotificationService notificationService;

    @PostMapping("/app-unlocks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportUnlock(CurrentUser user, @Valid @RequestBody AppUnlockReportRequest request) {
        notificationService.reportUnlock(user.id(), request.unlockMinutes().intValueExact(), request.limitExceeded());
    }

    @PostMapping("/app-lock-removal-recipients")
    public AppLockRemovalRecipientsResponse previewRemovalRecipients(CurrentUser user) {
        return notificationService.previewRemovalRecipients(user.id());
    }

    @PostMapping("/app-lock-removals")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmRemoval(CurrentUser user, @Valid @RequestBody AppLockRemovalRequest request) {
        notificationService.confirmRemoval(user.id(), request.recipientUserIds());
    }

    @PostMapping("/app-relock-reminders")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remindRelock(CurrentUser user) {
        notificationService.remindRelock(user.id());
    }
}
