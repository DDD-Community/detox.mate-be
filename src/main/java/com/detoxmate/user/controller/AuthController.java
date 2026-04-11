package com.detoxmate.user.controller;

import com.detoxmate.auth.dto.KakaoSocialLoginRequest;
import com.detoxmate.auth.dto.KakaoSocialLoginResponse;
import com.detoxmate.user.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/social/kakao")
    public KakaoSocialLoginResponse kakaoAuth(@RequestBody KakaoSocialLoginRequest request) {
        if (request.providerAccessToken() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "providerAccessToken is required");
        }

        return authService.loginWithKakao(request.providerAccessToken());
    }
}
