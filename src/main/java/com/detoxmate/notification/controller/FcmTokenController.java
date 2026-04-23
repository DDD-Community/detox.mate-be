package com.detoxmate.notification.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.notification.dto.RegisterFcmTokenRequest;
import com.detoxmate.notification.service.FcmTokenService;
import com.detoxmate.notification.util.TokenMasker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications/tokens")
@RequiredArgsConstructor
@Slf4j
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void register(CurrentUser user, @Valid @RequestBody RegisterFcmTokenRequest request) {
        log.info("[Notification][register-token] userId={}, platform={}, token={}",
                user.id(), request.platform(), TokenMasker.mask(request.token()));
        fcmTokenService.register(user.id(), request.token(),request.platform());
    }
}
