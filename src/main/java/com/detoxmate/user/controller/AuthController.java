package com.detoxmate.user.controller;

import com.detoxmate.auth.dto.KakaoSocialLoginRequest;
import com.detoxmate.auth.dto.AuthLoginResponse;
import com.detoxmate.auth.dto.RefreshTokenRequest;
import com.detoxmate.auth.dto.RefreshTokenResponse;
import com.detoxmate.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/social/kakao")
    public AuthLoginResponse kakaoAuth(@Valid @RequestBody KakaoSocialLoginRequest request) {
        return authService.loginWithKakao(request.providerAccessToken());
    }

    @PostMapping("/refresh")
    public RefreshTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }
}
