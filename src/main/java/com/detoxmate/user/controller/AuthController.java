package com.detoxmate.user.controller;

import com.detoxmate.auth.dto.KakaoSocialLoginRequest;
import com.detoxmate.auth.dto.KakaoSocialLoginResponse;
import com.detoxmate.auth.dto.RefreshTokenRequest;
import com.detoxmate.auth.dto.RefreshTokenResponse;
import com.detoxmate.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/social/kakao")
    public KakaoSocialLoginResponse kakaoAuth(@Valid @RequestBody KakaoSocialLoginRequest request) {
        return authService.loginWithKakao(request.providerAccessToken());
    }

    @PostMapping("/refresh")
    public RefreshTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }
}
