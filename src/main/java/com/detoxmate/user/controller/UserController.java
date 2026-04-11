package com.detoxmate.user.controller;

import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public MyProfileResponse getMe(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String accessToken = requireAccessToken(authorizationHeader);
        return userService.getMe(accessToken);
    }

    @DeleteMapping("/me")
    public void withdraw(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String accessToken = requireAccessToken(authorizationHeader);
        userService.withdraw(accessToken);
    }

    private String requireAccessToken(String authorizationHeader) {
        // Authorization 헤더 자체가 없으면 인증되지 않은 요청으로 본다.
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header is required");
        }

        return extractAccessToken(authorizationHeader);
    }

    private String extractAccessToken(String authorizationHeader) {
        String bearerPrefix = "Bearer ";

        if (!authorizationHeader.startsWith(bearerPrefix)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is required");
        }

        return authorizationHeader.substring(bearerPrefix.length());
    }
}
