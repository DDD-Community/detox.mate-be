package com.detoxmate.user.controller;

import com.detoxmate.auth.dto.KakaoSocialLoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/social/kakao")
    public String kakaoAuth(@RequestBody KakaoSocialLoginRequest request) {
        if (request.providerAccessToken() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "providerAccessToken is required");
        }

        return "ok";
    }
}
