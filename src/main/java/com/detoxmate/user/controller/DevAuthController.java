package com.detoxmate.user.controller;

import com.detoxmate.auth.dto.DevTestLoginRequest;
import com.detoxmate.auth.dto.AuthLoginResponse;
import com.detoxmate.user.service.DevAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "dev"})
@RestController
@RequestMapping("/dev/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final DevAuthService devAuthService;

    @PostMapping("/test-login")
    public AuthLoginResponse testLogin(@Valid @RequestBody DevTestLoginRequest request) {
        return devAuthService.testLogin(request.testUserKey());
    }
}
