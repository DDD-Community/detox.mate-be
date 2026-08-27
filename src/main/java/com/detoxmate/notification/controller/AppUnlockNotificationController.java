package com.detoxmate.notification.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.notification.service.AppUnlockNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications/app-unlock-requests")
@RequiredArgsConstructor
public class AppUnlockNotificationController {

    private final AppUnlockNotificationService appUnlockNotificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void request(CurrentUser currentUser) {
        appUnlockNotificationService.request(currentUser.id());
    }
}
