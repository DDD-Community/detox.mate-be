package com.detoxmate.user.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.dto.UpdateMyProfileRequest;
import com.detoxmate.user.dto.UpdatePushNotificationSettingRequest;
import com.detoxmate.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public MyProfileResponse getMe(CurrentUser currentUser) {
        return userService.getMe(currentUser.id());
    }

    @PatchMapping("/me")
    public MyProfileResponse updateMe(
            CurrentUser currentUser,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        return userService.updateMe(currentUser.id(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(CurrentUser currentUser) {
        userService.withdrawMe(currentUser.id());
    }

    @PatchMapping("/me/notifications")
    public ResponseEntity<Void> updatePushNotificationSetting(CurrentUser currentUser,
                                                              @Valid@RequestBody UpdatePushNotificationSettingRequest request) {
        userService.updatePushNotificationSetting(currentUser.id(), request.pushNotificationEnabled());
        return ResponseEntity.noContent().build();
    }
}
